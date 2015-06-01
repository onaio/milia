(ns milia.api.dataview
  (:require [milia.api.io :refer [make-url]]
            [milia.api.http :refer [parse-http]]))

(defn create
  "Create new dataview from existing dataset"
  [account params]
  (let [url (make-url "dataviews")
        options (merge params {:suppress-40x-exceptions? true}) ]
    (parse-http :post url account options)))
