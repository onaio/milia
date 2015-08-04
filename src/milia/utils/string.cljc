(ns milia.utils.string)

(defn is-null? [v]
  "Checks if a variable is null"
  (= "null" v))

(def is-not-null?
  (complement is-null?))
