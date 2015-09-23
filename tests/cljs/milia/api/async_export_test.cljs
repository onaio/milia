(ns milia.api.async-export-test
  (:require-macros [cljs.test :refer (is deftest testing)])
  (:require [cljs.test :as t]
            [milia.api.async-export :as async-export]))

(deftest handle-response-test
  (testing "handle export-url, on-export-url called, on-stop called"
    (let [sample-url "http://export.ed/file"
          response {:status 200
                    :body {:export_url sample-url}}
          mutable-obj #js {:stopped false}]
      (->> {:on-export-url #(aset mutable-obj "exportUrl" %)
            :on-stop #(aset mutable-obj "stopped" true)}
        (async-export/handle-response response))
      (is (= (.-exportUrl mutable-obj)
             sample-url))
      (is (= (.-stopped mutable-obj)
             true))))

  (testing "handle job-id, on-job-id called, on-stop called"
    (let [sample-job-id "012345"
          response {:status 200
                    :body {:export_url sample-job-id}}
          mutable-obj #js {:stopped false}]
      (->> {:on-job-id #(aset mutable-obj "jobId" %)
            :on-stop #(aset mutable-obj "stopped" true)}
        (async-export/handle-response response))
      (is (= (.-jobId mutable-obj)
             sample-job-id))
      (is (= (.-stopped mutable-obj)
             false)))))

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
