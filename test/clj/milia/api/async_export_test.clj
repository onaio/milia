(ns milia.api.async-export-test
  (:require [midje.sweet :refer :all]
            [milia.api.async-export :refer :all]
            [milia.api.http :refer [parse-http]]
            [milia.utils.remote :refer [make-url]]))

(fact "get-exports-per-form calls parse-http with the correct parameters"
      (get-exports-per-form :dataset-id :temp-token) => :api-response
      (provided
       (make-url (str "export?xform=" :dataset-id "&temp_token=" :temp-token)) 
       => :url
       (parse-http :get :url) => :api-response))

(fact "delete-export calls parse-http with the correct host url"
      (delete-export :export-id :temp-token) => :api-response
      (provided
       (make-url "export" (str :export-id "?temp_token=" :temp-token)) => :url
       (parse-http :delete :url) => :api-response))
