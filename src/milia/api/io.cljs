(ns milia.api.io
  (:import goog.net.IframeIo)
  (:require [cljs.core.async :refer [<! put! chan]]
            [cljs-hash.md5  :refer [md5]]
            [cljs-http.client :as http]
            [cljs-http.core :as http-core]
            [clojure.set :refer [rename-keys]]
            [clojure.string :refer [join split blank?]]
            [goog.net.cookies :as cks]
            [goog.events :as gev]
            [milia.utils.remote :refer [*credentials* hosts bad-token-msgs]]
            [milia.utils.seq :refer [in?]]
            [milia.utils.string :refer [is-not-null?]])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(defn build-http-options
  "Build http-options based on arguments."
  [http-options method no-cache?]
  (let [stateful-method? (in? [:post :put :patch] method)]
    ;; if JSON Params and stateful do not alter
    (if (and (:json-params http-options) stateful-method?)
      http-options
      (let [param-key (if stateful-method? :form-params :query-params)
            options+xhr (assoc-in http-options [param-key :xhr] true)]
        (if (and no-cache? (not stateful-method?))
          (assoc-in options+xhr [param-key :t] (md5 (.toString (.now js/Date))))
          options+xhr)))))

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
      http/wrap-method
      http/wrap-url))

(defn token->headers
  "Builds request headers for the HTTP request by adding
  Authorization, X-CSRFToken and Cache-control headers where necessary"
  [& {:keys [get-crsftoken? must-revalidate? accept-header]}]
  (let [temp-token (:temp-token @*credentials*)]
    (into {} [(when (and (not-empty temp-token) (is-not-null? temp-token))
                ["Authorization" (str "TempToken " temp-token)])
              (when must-revalidate?
                ["Cache-control" "must-revalidate"])
              (when-let [crsf-token (and get-crsftoken? (cks/get "csrftoken"))]
                ["X-CSRFToken" crsf-token])
              ["Accept" (or accept-header "application/json")]])))

(defn upload-file
  "Use goog.net.IframeIo to upload file. Receives an HTML form object,
  a core.async channel where result message will be put
  and (optionally) an id to include in the result message."
  [form chan & [id]]
  (let [io-obj   (IframeIo.)
        data-out {:io-obj io-obj}
        data-out (if id (assoc data-out :id id)
                     data-out)
        url      (.-action form)]
    (gev/listen io-obj (.-SUCCESS goog.net.EventType)
                #(put! chan (assoc data-out :success? true)))
    (gev/listen io-obj (.-ERROR goog.net.EventType)
                #(put! chan (assoc data-out :success? false)))
    (.sendFromForm io-obj form url)))

(defn http-request
  "Wraps cljs-http.client/request and redirects if status is 401"
  [request-fn & args]
  (let [response-channel (chan)]
    (go
      (let [original-response-channel (apply request-fn args)
            {:keys [status] :as response} (<! original-response-channel)]
        (if (= status 401)
          (set! js/window.location js/window.location)
          (put! response-channel response))))
    response-channel))
