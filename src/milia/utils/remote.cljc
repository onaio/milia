(ns milia.utils.remote
  (:require [clojure.string :refer [join]]
            [milia.utils.url :refer [url]]))

(def ^:dynamic *credentials*
  "Store credentials used to authenticate API requests.
   Based on existence in this atom credentials will be tried in top down order."
  (atom {:temp-token nil
         :token nil
         :username nil
         :password nil}))

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

(defn- url-join
  [host args]
  (join
   (conj [host] (apply url args))))

(defn make-url
  "Build an API url."
  [& postfix]
  (url-join (str (protocol-prefixed (:data @hosts)) "/api/v1") postfix))

(defn make-client-url
  "Build a Zebra url."
  [& postfix]
  (url-join (protocol-prefixed [(:client @hosts)]) postfix))

(defn make-j2x-url
  "Build an API url."
  [& postfix]
  (url-join (protocol-prefixed (:j2x @hosts)) postfix))
