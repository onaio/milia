(ns ona.utils.remote
  (:require [clojure.string :refer [join]]
            [ona.utils.url :refer [url]])
  (#+clj :use #+cljs :use-macros [ona.utils.macros :only [env-settings]]))

(def hosts
  ((keyword (env-settings :env))
   {:local {:ui "localhost:3000"
             :data (env-settings :ona-api-server-host)}
    :staging {:ui "zebra.ona.io"
               :data "stage.ona.io"}
    :preview {:ui "preview.ona.io"
               :data "ona.io"}
    :production {:ui "beta.ona.io"
                  :data "ona.io"
                  :forms "odk.ona.io"}
    :whodcp {:ui "beta.whodcp.org"
              :data "whodcp.org"}}))

(def local? (= (env-settings :env) "local"))

(defn is-local? [] local?)

(def protocol
  (env-settings :ona-api-server-protocol))

(defn- protocol-prefixed*
  "Prefix the resources with the protocol and format strings."
  [resources] (-> [protocol "://" resources] flatten join))

(def protocol-prefixed (memoize protocol-prefixed*))

(def j2x-host "j2x.ona.io")

(def thumbor-host "images.ona.io")

(def forms-host (protocol-prefixed (or (:forms hosts) (:data hosts))))

(def host-server (protocol-prefixed [(:data hosts) "/"]))

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
