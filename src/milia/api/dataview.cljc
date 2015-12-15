(ns milia.api.dataview
  (:refer-clojure :exclude [get update])
  (:require [milia.api.http :refer [parse-http]]
            [milia.utils.remote :refer [make-url]]))

(defn create
  "Create new dataview from existing dataset"
  [params]
  (let [url (make-url "dataviews")
        options {:form-params params}]
    (parse-http :post url :http-options options :suppress-4xx-exceptions? true)))

(defn get
  "Retrieves dataview object using dataview id"
  [dataview-id & {:keys [no-cache?]}]
  (let [url (make-url "dataviews" dataview-id)]
    (parse-http :get url :no-cache? no-cache? :suppress-4xx-exceptions? true)))

(defn data
  "Retrieves dataview data using dataview id"
  [dataview-id]
  (let [url (make-url "dataviews" dataview-id "data.json")]
    (parse-http :get url :suppress-4xx-exceptions? true :raw-response? true)))

(defn form
  "Retrieves form used to create dataview"
  [dataview-id]
  (let [url (make-url "dataviews" dataview-id "form.json")]
    (parse-http :get url :suppress-4xx-exceptions? true)))

(defn count-data
  "Counts data instances returned by dataview object"
  [dataview-id]
  (let [url (make-url "dataviews" dataview-id "data")
        options {:query-params {:count true}}]
    (parse-http :get url :http-options options :suppress-4xx-exceptions? true)))

(defn all
  "Retrieves all dataview objects"
  []
  (let [url (make-url "dataviews")]
    (parse-http :get url :suppress-4xx-exceptions? true)))

(defn update
  "Updates dataview object"
  [dataview-id params]
  (let [url (make-url "dataviews" dataview-id)
        options {:form-params params}]
    (parse-http :put url :http-options options :suppress-4xx-exceptions? true)))

(defn delete
  "Deletes dataview object"
  [dataview-id]
  (let [url (make-url "dataviews" dataview-id)]
    (parse-http :delete url)))
