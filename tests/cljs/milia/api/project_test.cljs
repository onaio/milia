(ns milia.api.project-test
  (:require-macros [cljs.test :refer (is deftest)])
  (:require [cljs.test :as t]
            [milia.api.http :refer [parse-http]]
            [milia.api.project :as project]))

(deftest test-update-public
  (is (not= (project/update-public :a false) nil)))

(deftest test-create-project
  ;; TODO find a way to test excpected variables in cljs
  ;; Something similar to midje provided or
  ;; https://github.com/hugoduncan/atticus
  (with-redefs [parse-http (fn [url] {:body "Created project"})]
    (is (= {:body "Created project"}
           (project/create {:name name
                            :public "False"
                            :metadata {:category "name"}} "username")))))
