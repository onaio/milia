(ns milia.api.charts
  (:require [milia.api.http :refer [parse-http]]
            [milia.utils.remote :refer [make-url]]))

(defn- suffix
  ([dataset-id]
   (str dataset-id ".json"))
  ([dataset-id field-name]
   (let [field-s (if (= field-name "all") "fields" "field_name")]
   (str dataset-id ".json?" field-s "=" field-name))))

(defn fields
  "Get list of chart fields for a specific dataset"
  [dataset-id]
  (let [url (make-url "charts" (suffix dataset-id))]
        (parse-http :get url)))

(defn chart
  "Get chart for a specific field in a dataset"
  ([dataset-id]
     (chart dataset-id "all"))
  ([dataset-id field-name]
      (let [url (make-url "charts" (suffix dataset-id field-name))]
        (parse-http :get url))))
