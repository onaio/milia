(ns milia.utils.remote
  (:require [clojure.string :refer [join]]
            [chimera.urls :refer [url]]))


(def ^:dynamic *credentials*
  "Store credentials used to authenticate API requests.
   Based on existence in this atom credentials will be tried in top down order."
  {:temp-token nil
   :token nil
   :username nil
   :password nil})

;; Token expiry API response messages
(def invalid-token-msg "Invalid token")
(def token-expired-msg "Token expired")
(def bad-token-msgs [invalid-token-msg token-expired-msg])

(def hosts
  "Store remote hosts that requests are made to."
  (atom {
         ;; used to create URLs that return to the client
         :client "zebra.ona.io"
         ;; Ona compatible API to request data from
         :data "stage.ona.io"
         ;; XLSReport server URL
         :j2x "j2x.ona.io"
         ;; protocol to use in all requests
         :request-protocol "https"}))

(defn protocol-prefixed
  "Prefix the resources with the protocol and format strings."
  [resources] (-> [(:request-protocol @hosts) "://" resources]
                  flatten join))

(def thumbor-server "https://images.ona.io")

(defn url-join
  [host args]
  (join
   (conj [host] (apply url args))))

(defn make-url
  "Build an API url."
  [& postfix]
  (url-join (str (protocol-prefixed (:data @hosts)) "/api/v1") postfix))

(defn make-client-url
  "Build a URL pointing to the client."
  [& postfix]
  #?(:clj
     (url-join (protocol-prefixed [(:client @hosts)]) postfix)
     :cljs
     (let [client-host (-> js/window (aget "location") (aget "origin"))]
       (url-join client-host postfix))))

(defn make-json-url [& args]
  "Like make-url, but ensures an ending in .json"
  (str (apply make-url args) ".json"))

(defn make-j2x-url
  "Build an API url."
  [& postfix]
  (url-join (protocol-prefixed (:j2x @hosts)) postfix))
