(ns milia.style-test
  (:require [clojure.java.shell :refer [sh]]
            [midje.sweet :refer :all]))

(fact "It shall pass the kibit static analysis checks"
      (:out (sh "lein" "kibit")) => "")

(fact "It shall pass the eastwood static analysis checks"
      (:out (sh "lein" "eastwood"))
      => (every-checker
          (contains "Warnings: 0")
          (contains "Exceptions thrown: 0")))

(fact "It shall pass the bikeshed checks"
      (sh "lein" "bikeshed") => (contains {:exit 0}))
