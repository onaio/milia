(ns milia.api.charts
  (:refer-clojure :exclude [get])
  (:require #?@(:cljs [[goog.string :as gstring]
                       [milia.api.http :refer [parse-http]]
                       [milia.utils.remote :refer [make-url]]]
                :clj [[milia.api.http :refer [parse-http]]
                      [milia.utils.remote :refer [make-url]]])))

#?(:cljs
   (def format gstring/format))

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


(defn get
  "Given a field name, return chart data associated with a dataset or dataview"
  [field-name & {:keys [dataview-id dataset-id group-by field-xpath]}]
  (let [id (or dataview-id dataset-id)
        base-url-template (cond
                            dataview-id "dataviews/%s/charts.json?field_name=%s"
                            field-xpath  "charts/%s.json?field_xpath=%s"
                            :else "charts/%s.json?field_name=%s")
        url-template (str base-url-template (when group-by "&group_by=%s"))
        url (make-url
             (format url-template id (or field-xpath field-name) group-by))]
    (parse-http :get url)))
