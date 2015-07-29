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
        http-options {:query-params (assoc params :xhr true)}]
    ;; Test http-options build correctly with no-cache? nil;
    ;; {:xhr true} should be added to :query-params.
    (is (= (io/build-http-options {:query-params params} nil) http-options))

    ;; Test http-options build correctly with no-cache? true;
    ;; {:xhr true} and {:t (timestamp)} should be added to :query-params.
    (is (contains? (-> (io/build-http-options {:query-params params} true)
                       :query-params keys set) 
                   :t))))
