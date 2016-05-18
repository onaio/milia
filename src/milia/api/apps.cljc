(ns milia.api.apps
  (:require [milia.api.http :refer [parse-http]]
            [milia.utils.remote :refer [make-rapidpro-url]]))

(defn get-rapidpro-data
  "Get rapidpro data given API Key and endpoint"
  [api-token endpoint]
  (let [url (make-rapidpro-url (str endpoint ".json"))]
    #?(:clj
       (parse-http :get url :http-options {:auth-token api-token}
                   :as-map? true
                   :raw-response? true
                   :suppress-4xx-exceptions? true))
    #?(:cljs
       (parse-http :get url :auth-token api-token
                   :as-map? true
                   :raw-response? true
                   :suppress-4xx-exceptions? true))))
