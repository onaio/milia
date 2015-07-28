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
            [milia.utils.seq :refer [in?]])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(defn build-http-options
  "Build http-options based on arguments."
  [http-options method no-cache?]
  (if no-cache?
    (assoc-in http-options [:query-params :t] (md5 (.toString (.now js/Date))))
    http-options))

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

(defn token->headers
  "Builds request headers for the HTTP request by adding
  Authorization, X-CSRFToken and Cache-control headers where necessary"
  [& {:keys [get-crsftoken? must-revalidate?]}]
  (let [temp-token (:temp-token @*credentials*)
        Authorization #(when temp-token
                         (assoc % "Authorization"
                                (str "TempToken " temp-token)))
        Cache-control #(when must-revalidate?
                         (assoc % "Cache-control" "must-revalidate"))
        X-CSRFToken #(when-let [crsf-token (and get-crsftoken?
                                                (cks/get "csrftoken"))]
                       (assoc % "X-CSRFToken" crsf-token))]
    (apply merge ((juxt Authorization Cache-control X-CSRFToken)
                  {"Accept" "application/json"}))))

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
