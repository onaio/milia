(ns milia.api.project-test
  (:require-macros [cljs.test :refer (is deftest)])
  (:require [cljs.test :as t]
            [milia.api.project :as project]))

(deftest test-update
  (is (not= (project/update-project :a :b {}) nil)))

(deftest test-update-params
  (is (not= (project/update-project :a :b {:key "value"}) nil)))

(deftest test-update-public
  (is (not= (project/update-public :a :b false) nil)))
