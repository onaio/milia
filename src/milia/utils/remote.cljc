(ns milia.utils.remote
  (:require [clojure.string :refer [join]]
            [chimera.urls :refer [url]]
            #?(:clj [environ.core :refer [env]])))

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

;; clj-http config options
#?(:clj (defn read-env-string
          "Read a string using env return nil if nil."
          [k]
          (if-let [v (env k)] (read-string v))))

#?(:clj (def debug-api? (read-env-string :debug-api)))
#?(:clj (def http-default-per-route
          (read-env-string :milia-http-default-per-route)))
#?(:clj (def http-threads (read-env-string :milia-http-threads)))

(def hosts
  "Store remote hosts that requests are made to."
  (atom {
         ;; used to create URLs that return to the client
         :client "zebra.ona.io"
         ;; Ona compatible API to request data from
         :data "stage-api.ona.io"
         ;; XLSReport server URL
         :j2x "j2x.ona.io"
         ;; protocol to use in all requests
         :request-protocol "https"}))

(def timeouts
  "Store customizable timeouts to use in the http libraries. In milliseconds."
  (atom {:conn-timeout 30000
         :socket-timeout 30000}))

(defn ^:export set-hosts
  "Swap values into hosts atom, requires data-host, other args are option but
   must be provided in order. If an option arg is nil it is ignored, and not
   swapped into hosts.

   Built to support setting hosts from JavaScript."
  [data-host & [client-host j2x-host request-protocol]]
  (swap! hosts merge
         (cond-> {:data data-host}
           (some? client-host) (assoc :client client-host)
           (some? j2x-host) (assoc :j2x j2x-host)
           (some? request-protocol)
           (assoc :request-protocol request-protocol))))

(defn ^:export set-credentials
  "Set the dynamic credentials to include the username and optionally
   any other arguments that are passed. If an argument is nil or not passed
   it will be set to nil in the credentials.

   Calling this from Clojure will break if not done from within a previous
   binding of the *credentials* variable.

   Built to support setting hosts from JavaScript."
  [username & [password temp-token token]]
  (set! *credentials* {:username username
                       :password password
                       :temp-token temp-token
                       :token token}))

(defn protocol-prefixed
  "Prefix the resources with the protocol and format strings."
  [resources] (-> [(:request-protocol @hosts) "://" resources]
                  flatten join))

(def thumbor-host "images.ona.io")
(def thumbor-server (protocol-prefixed thumbor-host))

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

(defn make-json-url
  "Like make-url, but ensures an ending in .json"
  [& args]
  (str (apply make-url args) ".json"))

(defn make-j2x-url
  "Build an API url."
  [& postfix]
  (url-join (protocol-prefixed (:j2x @hosts)) postfix))
