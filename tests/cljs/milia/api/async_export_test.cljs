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

  (testing "handle job-id, on-job-id called, on-stop NOT called"
    (let [sample-job-id "012345"
          response {:status 200
                    :body {:job_uuid sample-job-id}}
          mutable-obj #js {:stopped false}]
      (->> {:on-job-id #(aset mutable-obj "jobId" %)
            :on-stop #(aset mutable-obj "stopped" true)}
        (async-export/handle-response response))
      (is (= (.-jobId mutable-obj)
             sample-job-id))
      (is (= (.-stopped mutable-obj)
             false))))

  (testing "Failure causes on-error called, on-stop called"
    (let [sample-job-id "012345"
          response {:status 202
                    :body {:job_status async-export/export-failure-status-msg}}
          mutable-obj #js {:stopped false}]
      (->> {:on-error #(aset mutable-obj "error" %)
            :on-stop #(aset mutable-obj "stopped" true)}
           (async-export/handle-response response))
      (is (= (.-error mutable-obj)
             async-export/export-failure-status-msg))
      (is (= (.-stopped mutable-obj)
             true))))

  (testing "handle 400 error, on-error called, on-stop called"
    (let [sample-msg "No Internet Explorer allowed :)"
          response {:status 400
                    :body {:detail sample-msg}}
          mutable-obj #js {:stopped false}]
      (->> {:on-error #(aset mutable-obj "error" %)
            :on-stop  #(aset mutable-obj "stopped" true)}
        (async-export/handle-response response))
      (is (= (.-error mutable-obj)
             sample-msg))
      (is (= (.-stopped mutable-obj)
             true))))

  (testing "handle 503 error, on-error called, on-stop called"
    (let [sample-msg "Server is on holiday"
          response {:status 503
                    :body {:error sample-msg}}
          mutable-obj #js {:stopped false}]
      (->> {:on-error #(aset mutable-obj "error" %)
            :on-stop  #(aset mutable-obj "stopped" true)}
        (async-export/handle-response response))
      (is (= (.-error mutable-obj)
             sample-msg))
      (is (= (.-stopped mutable-obj)
             true)))))

(deftest build-export-suffix
  (testing "params rendered correctly"
    (let [fmt "format"
          meta-id "meta-id"
          data-id "data-id"
          version "20160627"
          url "http://test.me/"
          windows-compatible-csv true
          options {:meta-id meta-id
                   :data-id data-id
                   :version version
                   :windows-compatible-csv windows-compatible-csv
                   :redirect_uri url}
          ]
      (is (= (async-export/build-export-suffix async-export/export-async-url
                                               fmt options)
             (str async-export/export-async-url fmt
                  "&meta=" meta-id
                  "&data_id=" data-id
                  "&query='{\"_version\":\"20160627\"}'"
                  "&win_excel_utf8=" windows-compatible-csv
                  "&redirect_uri=" url))))))
