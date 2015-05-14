(ns milia.utils.url
  (:require [clojure.string :refer [split join]]))

(defn last-url-param
  "Get last parameter form url"
  [url]
  (let [last-param (-> url str (split #"/") last)]
    (-> last-param str (split #".json") first)))

(defn url
  "Append string args with slashes and prefix with a slash."
  [& args]
  (str "/" (join "/" args)))
