(ns milia.api.io-test
  (:import [goog.net Cookies])
  (:require-macros [cljs.test :refer (is deftest testing)])
  (:require [cljs.test :as t]
            [milia.api.io :as io]
            [milia.utils.remote :refer [*credentials*]]))

(def auth-token "auth-token")
(def params {:a 1})
(def json-params {:json-params params :with-credentials? false})
(def form-params {:form-params params :with-credentials? false})

(deftest build-request-headers
  (let [temp-token "z temp token"]
    (binding [*credentials* {:temp-token temp-token}]
      (is (= (io/token->headers :get-crsftoken? true
                                :must-revalidate? true)
             {"Accept" "application/json"
              "Authorization" (str "TempToken "  temp-token)
              "Cache-control" "must-revalidate"}))
      (is (= (io/token->headers :token temp-token)
             {"Accept" "application/json"
              "Authorization" (str "TempToken "  temp-token)}))

      (testing "do not add authentication header when token is null"
        (set! *credentials* {:temp-token "null"})
        (is (= (io/token->headers :token "null")
             {"Accept" "application/json"})))

      (testing "do not add authentication header when token is empty string"
        (set! *credentials* {:temp-token ""})
        (is (= (io/token->headers :token "")
             {"Accept" "application/json"})))

      (testing "add auth-token to Authorization header when auth-token
      exists"
        (is (= (io/token->headers :auth-token auth-token)
               {"Authorization" (str "Token "  auth-token)
                "Accept" "application/json"}))))))

(deftest build-http-options
  (let [get-http-options {:query-params params
                          :with-credentials? false}
        post-http-options form-params]

    (testing "for get request with no-cache? nil"
      (is (= (io/build-http-options {:query-params params} :get nil)
             get-http-options)))

    (testing "for get request with no-cache? true add {:t (timestamp)} to
              :query-params"
      (is (contains? (-> (io/build-http-options {:query-params params}
                                                :get
                                                true)
                         :query-params keys set) :t)))

    (testing "for post/patch/put request with no-cache? nil"
      (doseq [method [:post :patch :put]]
        (is (= (io/build-http-options form-params method nil)
               post-http-options))))

    (testing "for post/patch/put requests are never cached, should not add
              no-cache? even when passed"
      (doseq [method [:post :patch :put]]
        (is (= (io/build-http-options form-params method true)
               post-http-options))))

    (testing "for post/patch/put requests if json-params not no-cache?"
      (doseq [method [:post :patch :put]]
        (is (= (io/build-http-options {:json-params params} method true)
              json-params))))))

(defn- with-csrf-cookie
  [value f]
  (let [cookies (.getInstance Cookies)]
    (.set cookies "csrftoken" value)
    (try (f) (finally (.remove cookies "csrftoken")))))

(deftest csrf-headers
  (testing "precondition: the test page can set and read the cookie"
    (with-csrf-cookie "cookie-token"
      #(is (= "cookie-token" (.get (.getInstance Cookies) "csrftoken"))))
    (is (nil? (.get (.getInstance Cookies) "csrftoken"))))

  (testing "session token alone goes in X-CSRF-Token"
    (binding [*credentials* {:csrf-token "session-token"}]
      (is (= {"Accept" "application/json"
              "X-CSRF-Token" "session-token"}
             (io/token->headers :get-crsftoken? true)))))

  (testing "cookie alone fills both CSRF headers"
    (binding [*credentials* {}]
      (with-csrf-cookie "cookie-token"
        #(is (= {"Accept" "application/json"
                 "X-CSRFToken" "cookie-token"
                 "X-CSRF-Token" "cookie-token"}
                (io/token->headers :get-crsftoken? true))))))

  (testing "session token wins X-CSRF-Token, cookie keeps X-CSRFToken"
    (binding [*credentials* {:csrf-token "session-token"
                             :temp-token "temp"}]
      (with-csrf-cookie "cookie-token"
        #(is (= {"Accept" "application/json"
                 "Authorization" "TempToken temp"
                 "X-CSRFToken" "cookie-token"
                 "X-CSRF-Token" "session-token"}
                (io/token->headers :get-crsftoken? true))))))

  (testing "an empty session token falls back to the cookie"
    (binding [*credentials* {:csrf-token ""}]
      (with-csrf-cookie "cookie-token"
        #(is (= "cookie-token"
                (get (io/token->headers :get-crsftoken? true)
                     "X-CSRF-Token"))))))

  (testing "no CSRF headers when not asked for"
    (binding [*credentials* {:csrf-token "session-token"}]
      (with-csrf-cookie "cookie-token"
        #(is (= {"Accept" "application/json"}
                (io/token->headers :get-crsftoken? false)))))))
