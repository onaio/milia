(ns milia.utils.remote
  (:require [clojure.string :refer [join]]
            [milia.utils.url :refer [url]]))

(def hosts (atom {:ui "beta.ona.io"
                  :data "stage.ona.io"
                  :ona-api-server-protocol "https"}))

(def protocol
  (:ona-api-server-protocol @hosts))

(defn protocol-prefixed
  "Prefix the resources with the protocol and format strings."
  [resources] (-> [protocol "://" resources] flatten join))

(def j2x-host "j2x.ona.io")

(def thumbor-server "https://images.ona.io")

(defn- url-join
  [host args]
  (join
   (conj [host] (apply url args))))

(defn make-url
  "Build an API url."
  [& postfix]
  (url-join (str (protocol-prefixed (:data @hosts)) "/api/v1") postfix))

(defn make-zebra-url
  "Build a Zebra url."
  [& postfix]
  (url-join (protocol-prefixed [(:ui hosts) "/"]) postfix))

(defn make-j2x-url
  "Build an API url."
  [& postfix]
  (url-join (protocol-prefixed j2x-host) postfix))

(defn url-for-headers
  [headers suffix]
  (str protocol  "://" (headers "host") suffix))
