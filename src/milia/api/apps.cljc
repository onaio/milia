(ns milia.api.apps
  (:require [milia.api.http :refer [parse-http]]
            [milia.utils.remote :refer [url-join]]))

(def rapidpro-hosts {"rapidpro-ona" "rapidpro.ona.io"
                     "textit" "textit.in"})

(defn get-textit-host
  "Returns host URL"
  [server]
  (str (if-let [host-url (get rapidpro-hosts server)]
         (str "https://" host-url)
         server)))

(defn get-textit-api-url
  "Returns host url appended with API path"
  [server]
  (str server "/api/v2"))

(defn make-textit-url
  "Build a texit server API url."
  [server & postfix]
  (url-join (get-textit-api-url server) postfix))

(defn make-textit-service-url [server]
  (make-textit-url server "flow_starts.json"))

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
