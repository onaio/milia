(ns milia.api.open_data_test
  (:refer-clojure :exclude [update])
  (:require [midje.sweet :refer :all]
            [milia.api.open-data :refer :all]
            [milia.utils.remote :refer [make-url]]
            [milia.api.http :refer [parse-http]]))

(let [url       :fake-url
      object-id "fake-object-id"
      name      "fake-name"
      data-type "fake-data-type"
      uuid      "fake-uuid"]
  (facts "About create"
         (fact "Should create a new open-data object"
               (create object-id name data-type) => :something
               (provided
                (make-url "open-data.json") => url
                (parse-http
                 :post url
                 :http-options {:form-params
                                {:object_id object-id
                                 :name name
                                 :data_type data-type}}) => :something)))

  (facts "About update"
         (fact "Should update an existing open-data-object"
               (update object-id data-type uuid) => :something
               (provided
                (make-url "open-data" "fake-uuid.json") => url
                (parse-http
                 :patch url
                 :http-options {:form-params {:object_id object-id
                                              :data_type data-type}})
                => :something)))

  (facts "About getting open data object"
         (fact "Should get an existing open-data-object"
               (get-open-data-object uuid) => :something
               (provided
                (make-url "open-data" "fake-uuid.json") => url
                (parse-http :get url) => :something)))

  (facts "About delete"
         (fact "Should delete an existing open-data object"
               (delete uuid) => :something
               (provided
                (make-url "open-data" "fake-uuid.json") => url
                (parse-http :delete url) => :something)))

  (facts "About getting open data uuid"
         (fact "Should retrieve open-data uuid")
         (get-open-data-uuid object-id data-type) => :something
         (provided
          (make-url
           (str "open-data/uuid.json?object_id="
                object-id
                "&data_type="
                data-type))
          => url
          (parse-http :get url) => :something)))
