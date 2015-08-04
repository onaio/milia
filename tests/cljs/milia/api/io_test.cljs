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
              "Authorization" (str "TempToken "  temp-token)}))

      (testing "do not add authentication header when token is null"
        (swap! *credentials* assoc :temp-token "null")
        (is (= (io/token->headers :token "null")
             {"Accept" "application/json"})))

      (testing "do not add authentication header when token is empty string"
        (swap! *credentials* assoc :temp-token "")
        (is (= (io/token->headers :token "")
             {"Accept" "application/json"}))))))

(deftest build-http-options
  (let [params {:a 1}
        params-w-xhr-true (assoc params :xhr true)
        get-http-options {:query-params params-w-xhr-true}
        post-http-options {:form-params params-w-xhr-true}]

    (testing "for get request with no-cache? nil, should add {:xhr true}
              to :query-params"
      (is (= (io/build-http-options {:query-params params} :get nil)
             get-http-options)))

    (testing "for get request with no-cache? true should add {:xhr true}
              and {:t (timestamp)} to :query-params"

      (is (contains? (-> (io/build-http-options {:query-params params} :get true)
                         :query-params keys set) :t)))

    (testing "for post/patch/put request with no-cache? nil should add
              {:xhr true} to :form-params"
      (doseq [method [:post :patch :put]]
        (is (= (io/build-http-options {:form-params params} method nil)
               post-http-options))))

    (testing "for post/patch/put requests are never cached, should not add
              no-cache? even when passed"
      (doseq [method [:post :patch :put]]
        (is (= (io/build-http-options {:form-params params} method true)
               post-http-options))))

    (testing "for post/patch/put requests if json-params are passed should not
              add xhr or no-cache?"
      (doseq [method [:post :patch :put]]
        (is (= (io/build-http-options {:json-params params} method true)
              {:json-params params}))))))
