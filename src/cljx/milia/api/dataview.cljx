(ns milia.api.dataview
  (:require [milia.api.io :refer [make-url]]
            [milia.api.http :refer [parse-http]]))

(defn create
  "Create new dataview from existing dataset"
  [account params]
  (let [url (make-url "dataviews")
        options {:form-params params
                 :suppress-40x-exceptions? true}]
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

(defn count-data
  "Counts data instances returned by dataview object"
  [account dataview-id]
  (let [url (make-url "dataviews" dataview-id "data")
        options {:suppress-40x-exceptions? true
                 :query-params {:count true}}]
    (parse-http :get url account options)))

(defn all
  "Retrieves all dataview objects"
  [account]
  (let [url (make-url "dataviews")
        options {:suppress-40x-exceptions? true}]
    (parse-http :get url account options)))

(defn update
  "Updates dataview object"
  [account dataview-id params]
  (let [url (make-url "dataviews" dataview-id)
        options {:form-params params
                 :suppress-40x-exceptions? true}]
    (parse-http :put url account options)))

(defn delete
  "Deletes dataview object"
  [account dataview-id]
  (let [url (make-url "dataviews" dataview-id)]
    (parse-http :delete url account)))
