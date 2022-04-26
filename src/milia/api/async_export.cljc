(ns milia.api.async-export
  #?(:cljs (:require-macros [cljs.core.async.macros :refer [go]]))
  (:require [chimera.string :refer [is-not-null?]]
            #?@(:cljs [[goog.string.format]
                       [cljs.core.async :refer [<! chan put! timeout]]
                       [milia.utils.retry :refer [retry-parse-http]]
                       [milia.api.dataset :refer [type->endpoint]]])
            [clojure.string :refer [join]]
            [milia.api.http :refer [parse-http]]
            [milia.utils.remote :refer [make-url *credentials*]]))

(def export-async-url "export_async.json?format=")
(def export-failure-status-msg "FAILURE")
(def export-pending-status-msg "PENDING")
(def initial-polling-interval 5000) ; Async export polling interval in ms

(defn- temp-token-suffix
  "If a temp-token is set, add it as a query string parameter. The caller needs
   to explicity pass a question-mark or ampersand depending on whether this is
   the first or a subsequenty query parameter."
  [& s]
  (let [temp-token (:temp-token *credentials*)]
    (join (if (is-not-null? temp-token)
            (conj (vec s) "temp_token=" temp-token)
            s))))

(defn- handle-response
  "Handles API server's response and acts according to given callbacks."
  [{:as   response
    :keys [status body]}
   {:as   callbacks
    :keys [on-error on-export-url on-job-id on-stop]
    :or   {on-stop       (constantly nil)
           on-export-url identity
           on-error      identity
           on-job-id     identity}}]
  (let [{export-url   :export_url
         job-status   :job_status
         job-id       :job_uuid} body
        is-failed-status? #(= job-status export-failure-status-msg)
        error-detail (or (:detail body) (:error body) (:details body)
                         (when (is-failed-status?) job-status))]
    ;; sometimes API server returns an export-url quickly
    (when export-url
      (when (fn? on-export-url)
        (on-export-url export-url))
      (on-stop))
    ;; sometimes it doesn't. Instead, it may want us to wait and gives
    ;; us a job-uuid for the heavy-lifting export task.
    (when job-id
      (when (fn? on-job-id)
        (on-job-id job-id))
      false)
    ;; or it just gives an error
    (when (or (>= status 400)
              (is-failed-status?))
      (when (fn? on-error)
        (if (= status 403)
          (on-error error-detail (:url body))
          (on-error error-detail)))
      (on-stop))))

#?(:cljs
   (defn- monitor-async-export!
     "Repeatedly polls the async export progress for the given job_uuid,
    When export_url is returned, fires callback on-export-url."
     [dataset-id job-id & {:keys [on-error on-export-url
                                  data-type]}]
     (go
       (loop [polling-interval initial-polling-interval]
         (let [job-suffix (str "export_async.json?job_uuid=" job-id)
               job-url (make-url (type->endpoint data-type)
                                 dataset-id
                                 job-suffix)
               response (<! (retry-parse-http :get job-url :no-cache? true))]
          ;; Never use `on-job-id` here b/c `on-job-id` should only be
          ;; triggered once in `trigger-async-export!` where it starts
          ;; `monitor-async-export!` itself
           (when (not= (handle-response response {:on-stop #(constantly :stop)
                                                  :on-error on-error
                                                  :on-export-url on-export-url})
                       :stop)
             (<! (timeout polling-interval))
             (recur (* polling-interval 2))))))))

#?(:cljs
   (defn monitor-async-exports-per-form!
     "Repeatedly polls the export endpoint given a form_id while any of the
      export status is pending."
     [dataset-id  callback]
     (go
       (loop [polling-interval initial-polling-interval]
         (let [export-url (make-url (temp-token-suffix "export.json?xform="
                                                       dataset-id
                                                       "&"))
               {:keys [status body]} (<! (retry-parse-http :get
                                                           export-url
                                                           :no-cache?
                                                           true))
               pending-exports-list (vec (filter #(= (:job_status %)
                                                     export-pending-status-msg)
                                                 body))]
           (if (empty? pending-exports-list)
             (callback body)
             (do
               (<! (timeout polling-interval))
               (recur (* polling-interval 2)))))))))

(def version-key "_version")

(def export-option-keys
  ["meta" "data_id" "group_delimiter" "do_not_split_select_multiples"
   "include_hxl" "include_images" "remove_group_name" version-key "query"
   "export_id" "include_labels" "include_labels_only" "win_excel_utf8"
   "redirect_uri" "binary_select_multiples" "value_select_multiples"
   "show_choice_labels" "include_reviews" "language"])

(def export-option-values
  [:meta-id :data-id :group-delimiter :do-not-split-multi-selects?
   :include-hxl? :include-images? :remove-group-name? :version :query :export_id
   :include-labels? :labels-only? :windows-compatible-csv? :redirect-uri
   :binary-select-multiples? :value-select-multiples? :show-choice-labels?
   :include-reviews? :language])

(defn- get-param [key value]
  (if (= key version-key)
    #?(:cljs (goog.string.format "&query={\"%s\":\"%s\"}" key value))
    (str "&" key "=" value)))

(defn- add-param [key value]
  (when (or value (= value false))
    (get-param key value)))

(defn build-export-suffix
  "Build the export options string to pass to the Ona API."
  [url data-format & [export-options]]
  (->> export-options
       ((apply juxt export-option-values))
       (map add-param export-option-keys)
       #?(:cljs js/encodeURIComponent
         :clj identity)
       (concat [url data-format])
       (apply str)))

#?(:cljs
   (defn trigger-async-export!
     "Triggers async export and watches it via polling.
      Fires on-job-id callback on receving :job_uuid from server, then monitors
      job via polling.
      On receiving :export_url from server, on-export-url fired."
     ([dataset-id & [{:keys [data-type
                             data-format
                             export-options
                             ;; callbacks
                             on-job-id
                             on-export-url
                             on-error
                             on-done]}]]
      (go
        (let [export-suffix (build-export-suffix export-async-url
                                                 data-format
                                                 export-options)
              export-url (make-url (type->endpoint data-type)
                                   dataset-id
                                   export-suffix)
              response (<! (retry-parse-http :get export-url))
              inner-on-job-id (fn [job-id]
                                (on-job-id job-id)
                                (monitor-async-export!
                                 dataset-id job-id
                                 :on-export-url on-export-url
                                 :on-error on-error
                                 :data-type data-type))]
          (when on-done (on-done response))
          (handle-response response
                           {:on-error on-error
                            ;; new on-job-id that will be used in
                            ;; handle-response
                            :on-job-id (if on-done on-job-id inner-on-job-id)
                            :on-export-url on-export-url}))))))


#?(:cljs
   (defn get-async-export-url
     "Returns a channel, which will have the async export url when ready."
     [dataset-id data-format]
     (let [ch (chan 1)]
       (trigger-async-export! dataset-id {:data-format   data-format
                                          :on-export-url #(put! ch %)})
       ch)))

#?(:cljs
   (defn get-async-export-data
     "Returns a channel, which will have the async _data_
    downloaded using http-method when ready."
     [dataset-id fmt http-method & args]
     (go (let [url (<! (get-async-export-url dataset-id fmt))]
           (<! (apply parse-http (concat [http-method url] args)))))))

(defn get-exports-per-form
  "Get exports based on a form id."
  [dataset-id]
  (parse-http :get
              (make-url
               (temp-token-suffix "export.json?xform=" dataset-id "&"))))

(defn delete-export
  "Delete an export based on an export id"
  [export-id]
  (parse-http :delete
              (make-url "export" (temp-token-suffix export-id "?"))))
