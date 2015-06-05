(ns milia.api.dataview
  (:require [milia.api.io :refer [make-url]]
            [milia.api.http :refer [parse-http]]))

(defn create
  "Create new dataview from existing dataset"
  [account params]
  (let [url (make-url "dataviews")
        options (merge
                  {:form-params params}
                  {:suppress-40x-exceptions? true} )]
    (parse-http :post url account options)))

(defn show
  "Retrieves dataview object using dataview id"
  [account dataview-id]
  (let [url (make-url "dataviews" dataview-id)
        options {:suppress-40x-exceptions? true}]
    (parse-http :get url account options)))

(defn data
  "Retrieves dataview data using dataview id"
  [account dataview-id]
  (let [url (make-url "dataviews" dataview-id "data.json")
        options {:suppress-40x-exceptions? true :raw-response? true}]
    (parse-http :get url account options)))

(defn all
  "Retrieves all dataview objects"
  [account]
  (let [url (make-url "dataviews")
        options {:suppress-40x-exceptions? true}]
    (parse-http :get url account options)))
