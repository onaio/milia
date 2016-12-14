(ns milia.helpers
  (:require [slingshot.support :as support]))

(defn slingshot-exception
  "Mock a slingshot exception for Midje."
  [exception-map]
  (support/get-throwable
   (support/make-context
    exception-map
    (str "throw+: " exception-map)
    (Exception.)
    (support/stack-trace))))
