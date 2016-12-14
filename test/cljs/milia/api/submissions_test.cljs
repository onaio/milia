(ns milia.api.submissions-test
  (:require-macros [cljs.test :refer [is deftest async]]
                   [cljs.core.async.macros :refer [go]])
  (:require [milia.api.submissions :as sub]
            [cljs.test :as test]
            [cljs.core.async :refer [<! >! chan take!]]))

(deftest test-get-stats
  []
  (is (= (type (sub/get-stats 1 "_submitted_by" "submitted-by"))
         cljs.core.async.impl.channels/ManyToManyChannel)))
