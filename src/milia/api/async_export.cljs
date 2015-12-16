(ns milia.api.async-export
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.core.async :as async :refer [<! chan put! timeout]]
            [clojure.string :refer [join]]
            [milia.api.http :refer [parse-http]]
            [milia.utils.remote :refer [make-url]]
            [milia.utils.seq :refer [select-values]]))

(def export-async-url "export_async.json?format=")
(def export-failure-status-msg "FAILURE")

(defn- handle-response
  "Handles API server's response and acts according to given
  callbacks."
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
        error-detail (or (:detail body) (:error body)
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
        (on-job-id job-id)))
    ;; or it just gives an error
    (when (or (>= status 400)
              (is-failed-status?))
      (when (fn? on-error)
        (on-error error-detail))
      (on-stop))))

(defn- monitor-async-export!
  "Repeatedly polls the async export progress for the given job_uuid,
   When export_url is returned, fires callback on-export-url.
   `millis` is the number of milliseconds after which to poll again."
  [dataset-id job-id
   & [{:keys [on-error on-export-url
              is-filtered-dataview? millis]
       :or {:millis 1000}}]]
  (let [done-polling? (atom false)]
    (go
      (while (not @done-polling?)
        (let [job-suffix (str "export_async.json?job_uuid=" job-id)
              job-url (make-url dataset-id
                                (if is-filtered-dataview? "dataviews" "forms")
                                job-suffix)
              response (<! (parse-http :get job-url))]
          ;; Never use `on-job-id` here b/c `on-job-id` should only be
          ;; triggered once in `trigger-async-export!` where it starts
          ;; `monitor-async-export!` itself
          (handle-response response {:on-stop #(reset! done-polling? true)
                                     :on-error on-error
                                     :on-export-url on-export-url})
          (<! (timeout millis)))))))

(def export-option-keys
  ["meta" "data_id" "group_delimiter" "do_not_split_select_multiples"
   "remove_group_name" "_version" "query"])

(def export-option-values
  [:meta-id :data-id :group-delimiter :do-not-split-multi-selects?
   :remove-group-name? :version :query])

(defn- add-param [key value] (when value (str "&" key "=" value)))

(defn build-export-suffix
  "Build the export options string to pass to the Ona API."
  [url data-format & [export-options]]
  (->> export-options
       ((apply juxt export-option-values))
       (map add-param export-option-keys)
       (concat [url data-format])
       (apply str)))

(defn- trigger-async-export!
  "Triggers async export and watches it via polling.
   Fires on-job-id callback on receving :job_uuid from server, then monitors
   job via polling. On receiving :export_url from server, on-export-url fired."
  ([dataset-id
    & [{:keys [is-filtered-dataview? data-format export-options
               ;; callbacks
               on-job-id on-export-url on-error]}]]
   (go
     (let [export-suffix (build-export-suffix
                          export-async-url data-format export-options)
           export-endpoint (if is-filtered-dataview?
                             "dataviews" "forms")
           export-url (make-url export-endpoint dataset-id export-suffix)
           response (<! (parse-http :get export-url))
           ;; new on-job-id that will be used in handle-response
           inner-on-job-id
           (fn [job-id]
             (on-job-id job-id)
             (monitor-async-export!
              dataset-id job-id
              {:on-export-url on-export-url
               :on-error on-error
               :is-filtered-dataview? is-filtered-dataview?}))]
       (handle-response response
                        {:on-error on-error
                         :on-job-id inner-on-job-id
                         :on-export-url on-export-url})))))

(defn get-async-export-url
  [dataset-id data-format]
  "Returns a channel, which will have the async export url when ready."
  (let [ch (chan 1)]
    (trigger-async-export! dataset-id {:data-format   data-format
                                       :on-export-url #(put! ch %)})
    ch))

(defn get-async-export-data
  [dataset-id fmt http-method & args]
  "Returns a channel, which will have the async _data_
   downloaded using http-method when ready."
  (go (let [url (<! (get-async-export-url dataset-id fmt))]
        (<! (apply parse-http (concat [http-method url] args))))))
