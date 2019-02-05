(ns milia.api.open-data
  (:refer-clojure :exclude [update])
  (:require [milia.api.http :refer [parse-http]]
            [milia.utils.remote :refer [make-url]]))

(defn create
  "Create an open-data object.
   `object-id` is the numeric id of the object which can be an xform or a
   dataview.
   `name` is the name of the open-data object. It's advisable to use either
   the id_string or title for an xform or title for a dataview.
   `data-type` is class name in onadata and should either be
   'xform' or 'dataview'."
  [object-id name data-type]
  (parse-http :post
              (make-url "open-data.json")
              :http-options
              {:form-params
               {:object_id object-id
                :name name
                :data_type data-type}}))

(defn update
  "Updates an open-data object."
  [object-id data-type uuid]
  (parse-http :patch
              (make-url "open-data" (str uuid ".json"))
              :http-options
              {:form-params
               {:object_id object-id
                :data_type data-type}}))

(defn delete
  "Delete an open-data object"
  [uuid]
  (parse-http :delete (make-url "open-data" (uuid ".json"))))

(defn get-open-data-uuid
  "Get uuid of an open-data object."
  [object-id data-type]
  (parse-http
   :get
   (make-url
    (str "open-data/uuid.json?object_id=" object-id "&data_type=" data-type))))
