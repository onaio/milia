(ns milia.api.io-test
  (:require-macros [cljs.test :refer (is deftest testing)])
  (:require [cljs.test :as t]
            [milia.api.io :as io])
  (:use-macros [dommy.macros :only [node]]))

(deftest get-url-no-callback
  (let [url "http://jsontest.com"]
    (is (not= (io/query-helper! :get url) nil))))

(deftest get-url-callback
  (let [url "http://jsontest.com"]
    (is (not= (io/query-helper! :get url #() {}) nil))))

(deftest get-event-no-callback
  (let [event (js-obj "target" (node [:p]))]
    (is (not= (io/get-event event nil) nil))))

(deftest get-event-callback
  (let [event (js-obj "target" (node [:p]))]
    (is (not= (io/get-event event #()) nil))))

(deftest post-event-no-callback
  (let [event (js-obj "target" (node [:p]))]
    (is (not= (io/post-event event) nil))))

(deftest post-event-callback
  (let [event (js-obj "target" (node [:p]))]
    (is (not= (io/post-event event #()) nil))))

(deftest post-url-no-callback
  (let [url "http://jsontest.com"]
    (is (not= (io/query-helper! :post  url nil) nil))))

(deftest post-url-callback
  (let [url "http://jsontest.com"]
    (is (not= (io/query-helper! :post  url #()) nil))))

(deftest post-url-no-callback-params
  (let [url "http://jsontest.com"]
    (is (not= (io/query-helper! :post  url nil {:key "value"}) nil))))

(deftest post-url-callback-params
  (let [url "http://jsontest.com"]
    (is (not= (io/query-helper! :post  url #() {:key "value"}) nil))))

(deftest get-url-and-post-url-return-channels
  (let [url "http://jsontest.com"
        chnl cljs.core.async.impl.channels/ManyToManyChannel]
    (is (= chnl (type (io/get-url url))))
    (is (= chnl (type (io/raw-get-url url nil))))
    (is (= chnl (type (io/post-url url nil nil))))
    (is (= chnl (type (io/raw-post-url url {:key 1} nil))))))

(deftest check-invalid-or-expired-token
  (is (io/invalid-token? {:status 403 :body "Invalid token"}))
  (is (io/invalid-token? {:status 403 :body "Token expired"})))
