(ns ona.api.io
  (:import goog.net.IframeIo)
  (:require [cljs.core.async :refer [<! put!]]
            [cljs-hash.md5  :refer [md5]]
            [cljs-http.client :as http]
            [cljs-http.core :as http-core]
            [clojure.string :refer [join split blank?]]
            [dommy.core :as dommy]
            [goog.net.cookies :as cks]
            [goog.events :as gev]
            [ona.utils.remote :as remote])
  (:require-macros [cljs.core.async.macros :refer [go]]))

;; Shadowing CLJX function
(def make-url remote/make-url)
(defn make-json-url [& args]
  "Like make-url, but ensures an ending in .json"
  (let [bare-url (apply make-url args)] (str bare-url ".json")))

(defn refresh-token-url [username] (str "/" username "/temp-token"))

(defn validate-token-url [username] (str "/" username "/validate-token"))

(defn make-zebra-url
  "Build a url off of zebra"
  [& postfix]
  (let [zebra-host (-> js/window (aget "location") (aget "origin"))]
    (remote/url-join zebra-host postfix)))

(def raw-request
  "An almost 'batteries-included' request, similar to cljs-http.client/request.
   Contains everything except response decoding."
  (-> http-core/request
      http/wrap-accept
      http/wrap-form-params
      http/wrap-content-type
      http/wrap-json-params
      http/wrap-edn-params
      http/wrap-query-params
      http/wrap-basic-auth
      http/wrap-oauth
      http/wrap-android-cors-bugfix
      http/wrap-method
      http/wrap-url))

(defn raw-get
  [url & [req]]
  "Returns raw get output given a url, without decoding json/edn/transit output."
  (raw-request (merge req {:method :get :url url})))

(defn raw-post
  [url & [req]]
  "Returns raw post output given a url, without decoding json/edn/transit output."
  (raw-request (merge req {:method :post :url url})))

(defn token->headers
  ([token] (token->headers token false))
  ([token get-crsftoken?]
     (let [headers (when (and token (not (blank? token)))
                     {"Authorization" (str "TempToken " token)})]
       (if-let [crsf-token (and get-crsftoken? (cks/get "csrftoken"))]
         (assoc headers "X-CSRFToken" crsf-token)
         headers))))

(defn query-helper
  [method]
  "Returns a function which performs an http request with the given http-method.
   and returns a channel which will be populated on success / failure."
  (fn
    ([url]
       ((query-helper method) url nil nil))
    ([url query-params]
       ((query-helper method) url query-params nil))
    ([url query-params token & {:keys [no-cache?]}]
       (let [http-method ({:get http/get
                           :raw-get raw-get
                           :post http/post
                           :raw-post raw-post
                           :delete http/delete
                           :put http/put
                           :patch http/patch} method)
             param-key (if (contains? #{:put :patch} method)
                         :form-params :query-params)
             headers (token->headers token (= http-method http/delete))
             time-params (when no-cache? {:t (md5 (.toString (.now js/Date)))})
             query-params (merge query-params time-params {:xhr true})]
         (http-method url {:headers headers param-key query-params})))))

(def raw-get-url
  "GET a url (without decoding response); return channel w/ future response."
  (query-helper :raw-get))

(def get-url
  "GET a url AND decode the response; return channel w/ future response."
  (query-helper :get))

(def delete-url
  "Issues a DELETE request to a given url."
  (query-helper :delete))

(def patch-url
  "Issues a PATCH request for updating information."
  (query-helper :patch))

(def raw-post-url
  "GET a url (without decoding response); return channel w/ future response."
  (query-helper :raw-post))

(def post-url
  "GET a url AND decode the response; return channel w/ future response."
  (query-helper :post))

(def put-url
  (query-helper :put))

(defn query-helper!
  ([method url]
     (query-helper! url method nil))
  ([method url callback]
     (query-helper! method url callback nil))
  ([method url callback query-params]
    (query-helper! method url callback query-params nil))
  ([method url callback query-params token & opts]
     (go (let [response (<! ((query-helper method) url query-params token opts))]
           (when callback (callback response))))))

(defn get-event
  ([event]
    (get-event event nil))
  ([event callback]
    (let [url (dommy/attr (.-target event) :href)]
      (query-helper! :get url callback {})))
  ([event callback params]
    (let [url (dommy/attr (.-form (.-target event)) :action)]
      (query-helper! :get url callback params))))

(defn post-event
  ([event]
     (post-event event nil))
  ([event callback]
     (let [url (dommy/attr (.-target event) :href)]
       (query-helper! :post url callback))))

(defn invalid-token?
  "Checks if validate toke response returns invalid token message"
  [response]
  (when (and (= (:status response) 403)
             (or (= (:body response) "Invalid token")
                 (= (:body response) "Token expired")))
    true))

(defn validate-token
  "Validates users auth-token on client"
  [auth-token username]
  (go
   (if (or (nil? auth-token) (blank? auth-token) (= "null" auth-token))
     nil ;; Accessing as a non-signed in user
     (if (invalid-token? (<! (-> (validate-token-url username)
                                 (get-url nil nil :no-cache? true))))
       (let [{:keys [status body]} (<! (-> (refresh-token-url username)
                                           (get-url  nil nil :no-cache? true)))]
         (when (= status 200) body))
       auth-token))))

(defn upload-file
  "Use google library to upload file"
  [form chan]
  (let [io-obj (IframeIo.)
        url (.-action form)]
    (gev/listen io-obj (.-SUCCESS goog.net.EventType)
                #(put! chan {:success? true :io-obj io-obj}))
    (gev/listen io-obj (.-ERROR goog.net.EventType)
                #(put! chan {:success? false :io-obj io-obj}))
    (.sendFromForm io-obj form url)))
