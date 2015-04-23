(ns ona.utils.seq
  (:require [clojure.set :as cset]))

(defn has-keys?
  "True is map has all these keys."
  [m keys]
  (every? (partial contains? m) keys))

(defn in?
  "True if elem is in list, false otherwise."
  [list elem]
  (boolean (some #(= elem %) list)))