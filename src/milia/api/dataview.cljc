(ns milia.api.dataview
  (:require [milia.api.http :refer [parse-http]]
            [milia.utils.remote :refer [make-url]]))

(defn create
  "Create new dataview from existing dataset"
  [params]
  (let [url (make-url "dataviews")
        options {:form-params params
                 :suppress-40x-exceptions? true}]
    (parse-http :post url options)))

(defn get
  "Retrieves dataview object using dataview id"
  [dataview-id]
  (let [url (make-url "dataviews" dataview-id)
        options {:suppress-40x-exceptions? true}]
    (parse-http :get url options)))

(defn data
  "Retrieves dataview data using dataview id"
  [dataview-id]
  (let [url (make-url "dataviews" dataview-id "data.json")
        options {:suppress-40x-exceptions? true :raw-response? true}]
    (parse-http :get url options)))

(defn count-data
  "Counts data instances returned by dataview object"
  [dataview-id]
  (let [url (make-url "dataviews" dataview-id "data")
        options {:suppress-40x-exceptions? true
                 :query-params {:count true}}]
    (parse-http :get url options)))

(defn all
  "Retrieves all dataview objects"
  []
  (let [url (make-url "dataviews")
        options {:suppress-40x-exceptions? true}]
    (parse-http :get url options)))

(defn update
  "Updates dataview object"
  [dataview-id params]
  (let [url (make-url "dataviews" dataview-id)
        options {:form-params params
                 :suppress-40x-exceptions? true}]
    (parse-http :put url options)))

(defn delete
  "Deletes dataview object"
  [dataview-id]
  (let [url (make-url "dataviews" dataview-id)]
    (parse-http :delete url)))
