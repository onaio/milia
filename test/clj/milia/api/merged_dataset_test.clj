(ns milia.api.merged-dataset-test
  (:refer-clojure :exclude [get])
  (:require [midje.sweet :refer :all]
            [milia.api.http :refer [parse-http]]
            [milia.api.merged-dataset :refer :all]
            [milia.utils.remote :refer [make-url]]))

(def url :fake-url)
(def another-url :fake-url)
(def merged-dataset-id 1)
(def made-url (make-url "merged-datasets" (str merged-dataset-id "/data.json")))
(def options :something)
(def dataset-suffix (str merged-dataset-id "/data.json"))
  
(fact "about get merged-dataset"
      (get merged-dataset-id) => :response
      (provided
        (make-url "merged-datasets" (str merged-dataset-id ".json")) => url
        (parse-http :get url :no-cache? nil) => :response))

(fact "about merged-dataset"
    (data merged-dataset-id
            :format "json"
            :data-id nil
            :raw? true
            :must-revalidate? true
            :query-params nil) => :something
    (provided
      (make-url "merged-datasets" dataset-suffix) => url
      (parse-http :get another-url
                  :http-options {:query-params nil}
                  :raw-response? true
                  :must-revalidate? true
                  :accept-header nil) => :something))
