(ns milia.api.async-export
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.core.async :as async :refer [<! chan put! timeout]]
            [milia.api.io :as io]))

(defn- monitor-async-export!
  [auth-token dataset-id job-id fmt on-export-url
   & {:keys [:millis] :or {:millis 1000}}]
  "Repeatedly polls the async export progress for the given job_uuid,
   When export_url is returned, fires callback on-export-url.
   `millis` is the number of milliseconds after which to poll again."
  (let [done-polling? (atom false)]
    (go
     (while (not @done-polling?)
       (let [job-suffix (str "export_async.json?job_uuid=" job-id)
             job-url (io/make-url "forms" dataset-id job-suffix)
             response (:body (<! (io/get-url job-url {} auth-token)))]
         (when-let [export-url (:export_url response)]
           (on-export-url export-url)
           (reset! done-polling? true))
         (<! (timeout millis)))))))

(defn- trigger-async-export!
  "Triggers async export and watches it via polling.
   Fires on-job-id callback on receving :job_uuid from server, then monitors
   job via polling. On receiving :export_url from server, on-export-url fired."
  ([auth-token dataset-id fmt on-job-id on-export-url]
   (trigger-async-export! auth-token dataset-id fmt on-job-id on-export-url nil))
  ([auth-token dataset-id fmt on-job-id on-export-url {:keys [meta-id
                                                              data-id
                                                              remove-group-name?
                                                              version
                                                              is-filtered-dataview?]}]
   (go
     (let [export-suffix (str "export_async.json?format=" fmt
                              (when meta-id (str "&meta="meta-id))
                              (when data-id (str "&data_id="meta-id))
                              (when remove-group-name? (str "&remove_group_name="remove-group-name?))
                              (when version (str "&_version="version)))
           export-endpoint (if is-filtered-dataview? "dataviews" "forms")
           export-url (io/make-url export-endpoint dataset-id export-suffix)
           response (:body (<! (io/get-url export-url {} auth-token)))]
       (when-let [export-url (:export_url response)]
         (on-export-url export-url))
       (when-let [job-id (:job_uuid response)]
         (on-job-id job-id)
         (monitor-async-export! auth-token dataset-id job-id fmt on-export-url))))))

(defn get-async-export-url
  [auth-token dataset-id fmt]
  "Returns a channel, which will have the async export url when ready."
  (let [ch (chan 1)]
    (trigger-async-export! auth-token dataset-id fmt identity #(put! ch %))
    ch))

(defn get-async-export-data
  [auth-token dataset-id fmt http-method]
  "Returns a channel, which will have the async _data_
   downloaded using http-method when ready."
  (go (let [url (<! (get-async-export-url auth-token dataset-id fmt))]
        (<! ((io/query-helper http-method) url {} auth-token)))))
