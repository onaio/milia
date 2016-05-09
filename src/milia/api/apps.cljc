(ns milia.api.apps
  (:require [milia.api.http :refer [parse-http]]
            [milia.utils.remote :refer [make-rapidpro-url]]))

(defn get-rapidpro-flows
  "Get rapidpro flow given API Key"
  [api-token]
  (let [url (make-rapidpro-url "flows")]
    (parse-http :get url :token api-token)))
