(ns milia.api.io-test
  (:require-macros [cljs.test :refer (is deftest testing)])
  (:require [cljs.test :as t]
            [milia.api.io :as io]
            [milia.utils.remote :refer [*credentials*]]))

(deftest build-request-headers
  (let [temp-token "z temp token"]
    (binding [*credentials* (atom {:temp-token temp-token})]
      (is (= (io/token->headers :get-crsftoken? true
                                :must-revalidate? true)
             {"Accept" "application/json"
              "Authorization" (str "TempToken "  temp-token)
              "Cache-control" "must-revalidate"}))
      (is (= (io/token->headers :token temp-token)
             {"Accept" "application/json"
              "Authorization" (str "TempToken "  temp-token)})))))


(deftest build-http-options
  (let [params {:a 1}
        params-w-xhr-true (assoc params :xhr true)
        get-http-options {:query-params params-w-xhr-true}
        post-http-options {:form-params params-w-xhr-true}]
    ;; Test http-options build correctly for :get request with no-cache? nil;
    ;; {:xhr true} should be added to :query-params.
    (is (= (io/build-http-options {:query-params params} :get nil) get-http-options))

    ;; Test http-options build correctly for :get request with no-cache? true;
    ;; {:xhr true} and {:t (timestamp)} should be added to :query-params.
    (is (contains? (-> (io/build-http-options {:query-params params} :get true)
                       :query-params keys set)
                   :t))

    ;; Test http-options build correctly for :post/:patch/:put request with no-cache? nil;
    ;; {:xhr true} should be added to :form-params.
    (is (= (io/build-http-options {:form-params params} :post nil) post-http-options))
    (is (= (io/build-http-options {:form-params params} :patch nil) post-http-options))
    (is (= (io/build-http-options {:form-params params} :put nil) post-http-options))

    ;; POST requests are never cached, this test confirms timestamp is not added when no-cache?
    ;; is set to true.
    (is (= (io/build-http-options {:form-params params} :post true) post-http-options))))
