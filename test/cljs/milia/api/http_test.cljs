(ns milia.api.http-test
  (:require-macros [cljs.test :refer (is deftest testing)])
  (:require [cljs.test :as t]
            [milia.api.http :refer [parse-http]]
            [milia.api.io :as io]
            [milia.utils.remote :refer [*credentials*]]))

(defn- sent-headers
  [method]
  (let [sent (atom nil)]
    (with-redefs [io/http-request (fn [_ request]
                                    (reset! sent request)
                                    :response-channel)]
      (parse-http method "https://api.example.org/api/v1/forms"))
    (:headers @sent)))

(deftest parse-http-csrf-header
  (binding [*credentials* {:csrf-token "session-token"}]
    (testing "precondition: the stub captured a request"
      (is (map? (sent-headers :post))))
    (testing "non-GET requests carry the session CSRF token"
      (doseq [method [:post :put :patch :delete]]
        (is (= "session-token" (get (sent-headers method) "X-CSRF-Token")))))
    (testing "GET requests carry no CSRF token"
      (is (nil? (get (sent-headers :get) "X-CSRF-Token"))))))
