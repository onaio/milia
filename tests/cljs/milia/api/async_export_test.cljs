(ns milia.api.async-export-test
  (:require-macros [cljs.test :refer (is deftest testing)])
  (:require [cljs.test :as t]
            [milia.api.async-export :as async-export]))

(deftest build-export-suffix
  (testing "params rendered correctly"
    (let [fmt "format"
          meta-id "meta-id"
          data-id "data-id"
          options {:meta-id meta-id
                   :data-id data-id}]
      (is (= (async-export/build-export-suffix fmt options)
             (str "export_async.json?format="
                  fmt
                  "&meta="
                  meta-id
                  "&data_id="
                  data-id))))))
