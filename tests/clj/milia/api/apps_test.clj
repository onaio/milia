(ns milia.api.apps-test
  (:require [midje.sweet :refer :all]
            [milia.api.apps :refer :all]
            [milia.api.http :refer [parse-http]]
            [milia.utils.remote :refer [make-rapidpro-url]]))

(fact "get-all calls parse-http with the correct parameters"
      (get-rapidpro-flows "a1b2c3") => :api-response
      (provided
       (make-rapidpro-url "flows") => :url
       (parse-http :get :url :token "a1b2c3") => :api-response))
