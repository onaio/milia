(ns milia.api.submissions
  (:require [milia.api.http :refer [parse-http]]
            [milia.utils.remote :refer [make-url]]))

(defn get-stats
  "Int String String -> Channel containing HttpResponse"
  [dataset-id group-by name]
  (parse-http
   :get
   (make-url "stats/submissions"
             (str dataset-id ".json?group=" group-by "&name=" name))))
