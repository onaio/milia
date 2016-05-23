(ns milia.api.apps-test
  (:require [midje.sweet :refer :all]
            [milia.api.apps :refer :all]
            [milia.api.http :refer [parse-http]]))

(def server "rapidpro-ona")
(def endpoint "flows")
(def api-key "a1b2c3")

(fact "get-all calls parse-http with the correct parameters"
      (get-textit-data api-key endpoint server) => :api-response
      (provided
       (make-textit-url server (str endpoint ".json")) => :url
       (parse-http :get :url :http-options {:auth-token api-key}
                   :as-map? true
                   :raw-response? true
                   :suppress-4xx-exceptions? true) => :api-response))
