(ns ona.utils.remote
  (:require [clojure.string :refer [join]]
            [ona.utils.url :refer [url]]))

(def hosts (atom {:ui "beta.ona.io"
                  :data "stage.ona.io"
                  :ona-api-server-protocol "https"}))

(def protocol
  (:ona-api-server-protocol @hosts))

(defn- protocol-prefixed*
  "Prefix the resources with the protocol and format strings."
  [resources] (-> [protocol "://" resources] flatten join))

(def protocol-prefixed (memoize protocol-prefixed*))

(def j2x-host "j2x.ona.io")

(def thumbor-host "images.ona.io")

(def forms-host (protocol-prefixed (or (:forms @hosts) (:data @hosts))))

(def host-server (protocol-prefixed [(:data @hosts) "/"]))

(def j2x-host-server (protocol-prefixed j2x-host))

(def thumbor-server (protocol-prefixed thumbor-host))

(defn- url-join
  [host args]
  (join
   (conj [host] (apply url args))))

(defn make-url
  "Build an API url."
  [& postfix]
  (url-join (str host-server "api/v1") postfix))

(defn make-zebra-url
  "Build a Zebra url."
  [& postfix]
  (url-join (protocol-prefixed [(:ui hosts) "/"]) postfix))

(defn make-j2x-url
  "Build an API url."
  [& postfix]
  (url-join j2x-host-server postfix))

(defn url-for-headers
  [headers suffix]
  (str protocol  "://" (headers "host") suffix))
