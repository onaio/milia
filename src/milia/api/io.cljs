(ns milia.api.io
  (:import goog.net.IframeIo)
  (:require [cljs.core.async :refer [<! put!]]
            [cljs-hash.md5  :refer [md5]]
            [cljs-http.client :as http]
            [cljs-http.core :as http-core]
            [clojure.set :refer [rename-keys]]
            [clojure.string :refer [join split blank?]]
            [goog.net.cookies :as cks]
            [goog.events :as gev]
            [milia.utils.remote :as remote])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(defn build-http-options
  "Build http-options based on arguments."
  [http-options method no-cache?]
  ;; For :post / :put / :patch, we need :form-params,
  ;; for the rest :query-params
  (let [http-options (if (contains? #{:post :put :patch} method)
                       http-options
                       (rename-keys http-options
                                    {:form-params :query-params}))]
    (if no-cache?
      (assoc-in http-options [:query-params :t] (md5 (.toString (.now js/Date))))
      http-options)))

(defn make-json-url [& args]
  "Like make-url, but ensures an ending in .json"
  (let [bare-url (apply remote/make-url args)] (str bare-url ".json")))

(defn make-client-url
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

(defn token->headers
  "Builds request headers for the HTTP request by adding
  Authorization, X-CSRFToken and Cache-control headers where necessary"
  [& {:keys [get-crsftoken? must-revalidate?]}]
  (let [temp-token (:temp-token @remote/*credentials*)
        Authorization #(when temp-token
                         (assoc % "Authorization"
                                (str "TempToken " temp-token)))
        Cache-control #(when must-revalidate?
                         (assoc % "Cache-control" "must-revalidate"))
        X-CSRFToken #(when-let [crsf-token (and get-crsftoken?
                                                (cks/get "csrftoken"))]
                       (assoc % "X-CSRFToken" crsf-token))]
    (apply merge ((juxt Authorization Cache-control X-CSRFToken) {}))))

(defn invalid-token?
  "Checks if validate toke response returns invalid token message"
  [response]
  (when (and (= (:status response) 403)
             (or (= (:body response) "Invalid token")
                 (= (:body response) "Token expired")))
    true))

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
