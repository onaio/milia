(ns milia.api.merged-dataset
  (:refer-clojure :exclude [get])
  (:require [milia.api.http :refer [parse-http]]
            [milia.utils.remote :refer [make-url]]))

(defn get
  "Show dataset metadata."
  [merged-dataset-id & {:keys [no-cache?]}]
  (let [url (make-url "merged-datasets" (str merged-dataset-id ".json"))]
    (parse-http :get url :no-cache? no-cache?)))

(defn ^:export data
  "Return the data associated with a dataset."
  [merged-dataset-id & {:keys [format
                               raw?
                               must-revalidate?
                               accept-header
                               query-params
                               data-id]
                        #?@(:cljs [:or {:format "json"}])}]
  (let [url     (make-url "merged-datasets"
                           merged-dataset-id
                           (when format (str "data" data-id "." format)))
        options {:query-params query-params}]
    (parse-http :get              url
                :http-options     options
                :raw-response?    raw?
                :must-revalidate? must-revalidate?
                :accept-header    accept-header)))