(ns milia.api.apps
  (:require [chimera.seq :refer [in?]]
            [milia.api.http :refer [parse-http]]
            [milia.utils.remote :refer [url-join]]))

(def rapidpro-hosts {"rapidpro-ona" "rapidpro.ona.io"
                     "textit" "textit.in"})

(defn get-host-url
  [server]
  (str
    (if-let [host-url (get rapidpro-hosts server)]
        (str "https://" host-url)
      server)
    "/api/v2"))

(defn make-textit-url
  "Build a texit server API url."
  [server & postfix]
  (url-join (get-host-url server)  postfix))

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
