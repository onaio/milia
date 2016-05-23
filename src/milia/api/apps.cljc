(ns milia.api.apps
  (:require [milia.api.http :refer [parse-http]]
            [milia.utils.remote :refer [url-join]]))

(defn server-url
  [server]
  (server {:rapidpro-ona "rapidpro.ona.io"
           :textit "textit.in"
           :rapidpro "app.rapidpro.io"}))

(defn make-textit-url
  "Build a texit server API url."
  [server & postfix]
  (url-join (str "https://" (-> server keyword server-url) "/api/v1")
            postfix))

(defn get-textit-data
  "Get data from textit server given server URL, endpoint & API Key."
  [api-token endpoint server]
  (let [url (make-textit-url server (str endpoint ".json"))]
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
