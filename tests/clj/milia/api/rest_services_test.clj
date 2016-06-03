(ns milia.api.rest-services-test
  (:require [midje.sweet :refer :all]
            [milia.api.rest-services :refer :all]
            [milia.api.http :refer [parse-http]]
            [milia.utils.remote :refer [make-url]]))

(def xform-id 1)
(def rest-service-id 1)
(def base-url (make-url "restservices"))
(def single-rest-service-url (make-url "restservices" rest-service-id))
(def form-rest-service-url (make-url (str "restservices?xform=" xform-id)))
(def service-url "http://example.org/webhook")

(fact "create calls parse-http with the correct parameters"
      (create xform-id "generic_json" service-url) => :api-response
      (provided
       (parse-http :post
                   base-url
                   :http-options
                   {:form-params {:xform xform-id
                                  :name "generic_json"
                                  :service_url service-url}}) => :api-response))

(fact "delete calls parse-http with the correct parameters"
      (delete rest-service-id) => :api-response
      (provided
       (parse-http :delete single-rest-service-url) => :api-response))

(fact "get-all calls parse-http with the correct parameters"
      (get-all) => :api-response
      (provided
       (parse-http :get base-url) => :api-response))

(fact "get-by-id calls parse-http with the correct parameters"
      (get-by-id rest-service-id :no-cache? true) => :api-response
      (provided
       (parse-http :get single-rest-service-url
                   :no-cache? true) => :api-response))

(fact "get-by-form-id calls parse-http with the correct parameters"
      (get-by-form-id xform-id :no-cache? true) => :api-response
      (provided
       (parse-http :get form-rest-service-url
                   :no-cache? true) => :api-response))

(fact "update calls parse-http with the correct parameters"
      (update-restservice rest-service-id xform-id "generic_json"
                          service-url) =>
      :api-response
      (provided
       (parse-http :put
                   single-rest-service-url
                   :http-options
                   {:form-params
                    {:xform xform-id
                     :name "generic_json"
                     :service_url service-url}}) => :api-response))
