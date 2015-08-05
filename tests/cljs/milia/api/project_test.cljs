(ns milia.api.project-test
  (:require-macros [cljs.test :refer (is deftest)])
  (:require [cljs.test :as t]
            [milia.api.project :as project]))

(deftest test-update-public
  (is (not= (project/update-public :a false) nil)))
