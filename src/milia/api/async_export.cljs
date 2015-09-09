(ns milia.api.async-export
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.core.async :as async :refer [<! chan put! timeout]]
            [milia.api.http :refer [parse-http]]
            [milia.utils.remote :refer [make-url]]))

(defn- monitor-async-export!
  [dataset-id job-id fmt on-export-url is-filtered-dataview?
   & {:keys [:millis] :or {:millis 1000}}]
  "Repeatedly polls the async export progress for the given job_uuid,
   When export_url is returned, fires callback on-export-url.
   `millis` is the number of milliseconds after which to poll again."
  (let [done-polling? (atom false)]
    (go
     (while (not @done-polling?)
       (let [job-suffix (str "export_async.json?job_uuid=" job-id)
             job-url (make-url (if is-filtered-dataview? "dataviews" "forms")
                               dataset-id job-suffix)
             response (:body (<! (parse-http :get job-url)))]
         (when-let [export-url (:export_url response)]
           (on-export-url export-url)
           (reset! done-polling? true))
         (<! (timeout millis)))))))

(defn- trigger-async-export!
  "Triggers async export and watches it via polling.
   Fires on-job-id callback on receving :job_uuid from server, then monitors
   job via polling. On receiving :export_url from server, on-export-url fired."
  ([dataset-id fmt on-job-id on-export-url]
   (trigger-async-export! dataset-id fmt on-job-id on-export-url nil))
  ([dataset-id fmt on-job-id on-export-url
    {:keys [meta-id
            data-id
            remove-group-name?
            do-not-split-multi-selects?
            group-delimiter
            version
            is-filtered-dataview?]}]
   (go
     (let [export-suffix
           (str "export_async.json?format=" fmt
                (when meta-id (str "&meta="meta-id))
                (when data-id (str "&data_id="meta-id))
                (when group-delimiter
                  (str "&group_delimiter=" group-delimiter))
                (when do-not-split-multi-selects?
                  (str "&dont_split_select_multiples=true"))
                (when remove-group-name?
                  (str "&remove_group_name="remove-group-name?))
                (when version (str "&_version="version)))
           export-endpoint (if is-filtered-dataview? "dataviews" "forms")
           export-url (make-url export-endpoint dataset-id export-suffix)
           response (:body (<! (parse-http :get export-url)))]
       (when-let [export-url (:export_url response)]
         (on-export-url export-url))
       (when-let [job-id (:job_uuid response)]
         (on-job-id job-id)
         (monitor-async-export! dataset-id
                                job-id
                                fmt
                                on-export-url
                                is-filtered-dataview?))))))

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
