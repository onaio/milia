(ns milia.api.async-export
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.core.async :as async :refer [<! chan put! timeout]]
            [clojure.string :refer [join]]
            [milia.api.http :refer [parse-http]]
            [milia.utils.remote :refer [make-url]]
            [milia.utils.seq :refer [select-values]]))

(defn- monitor-async-export!
  [dataset-id job-id
   & {:keys [on-error on-export-url is-filtered-dataview? millis]
      :or {:millis 1000}}]
  "Repeatedly polls the async export progress for the given job_uuid,
   When export_url is returned, fires callback on-export-url.
   `millis` is the number of milliseconds after which to poll again."
  (let [done-polling? (atom false)]
    (go
     (while (not @done-polling?)
       (let [job-suffix (str "export_async.json?job_uuid=" job-id)
             job-url (make-url (if is-filtered-dataview? "dataviews" "forms")
                               dataset-id job-suffix)
             response (:body (<! (parse-http :get job-url)))
             {export-url :export_url :keys [error]} response]
         (when (and export-url (fn? on-export-url))
           (on-export-url export-url)
           (reset! done-polling? true))
         (when (and error (fn? on-error))
           (on-error error)
           (reset! done-polling? true))
         (<! (timeout millis)))))))

(def export-option-keys
  ["meta" "data_id" "group_delimiter" "do_not_split_select_multiples"
   "remove_group_name" "_version"])
(def export-option-values
  [:meta-id :data-id :remove-group-name? :do-not-split-multi-selects?
   :group-delimiter :version])

(defn- add-param [key value] (when value (str "&" key "=" value)))

(defn build-export-suffix
  "Build the export options string to pass to the Ona API."
  [fmt options]
  (apply str (concat ["export_async.json?format=" fmt]
                     (map add-param
                          export-option-keys
                          (select-values options export-option-values)))))

(defn- trigger-async-export!
  "Triggers async export and watches it via polling.
   Fires on-job-id callback on receving :job_uuid from server, then monitors
   job via polling. On receiving :export_url from server, on-export-url fired."
  ([dataset-id
    & [{:keys [is-filtered-dataview? data-format
               ;; callbacks
               on-job-id on-export-url on-error]
        :as options}]]
   (go
     (let [export-suffix (build-export-suffix data-format options)
           export-endpoint (if is-filtered-dataview?
                             "dataviews" "forms")
           export-url (make-url export-endpoint dataset-id export-suffix)
           response (:body (<! (parse-http :get export-url)))
           {export-url :export_url
            job-id :job_uuid
            job-status :job_status} response]
       (when (and export-url (fn? on-export-url))
         (on-export-url export-url))
       (when (and job-id (fn? on-job-id))
         (on-job-id job-id)
         (->> {:on-export-url         on-export-url
               :on-error              on-error
               :is-filtered-dataview? is-filtered-dataview?}
           (monitor-async-export! dataset-id job-id)))))))

(defn get-async-export-url
  [dataset-id fmt]
  "Returns a channel, which will have the async export url when ready."
  (let [ch (chan 1)]
    (trigger-async-export! dataset-id fmt identity #(put! ch %))
    ch))

(defn get-async-export-data
  [dataset-id fmt http-method & args]
  "Returns a channel, which will have the async _data_
   downloaded using http-method when ready."
  (go (let [url (<! (get-async-export-url dataset-id fmt))]
        (<! (apply parse-http (concat [http-method url] args))))))
