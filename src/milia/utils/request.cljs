(ns milia.utils.request
  (:require [cljs.core.async :as async :refer [<! put! chan]])
  (:require-macros [cljs.core.async.macros :refer [go]]))

;; The below are matched to API responses
(def invalid-token-msg "Invalid token")
(def token-expired-msg "Token expired")

(defn refresh-token-url [username] (str "/" username "/temp-token"))

(defn request
  "Wraps cljs-http.client/request and redirects if status is 401"
  [request-fn & args]
  (let [response-channel (chan)]
    (go
      (let [original-response-channel (apply request-fn args)
            {:keys [status] :as response} (<! original-response-channel)]
        (condp = status
          401 (let [{{detail :detail} :body} response
                    reload-page #(set! js/window.location js/window.location)
                    login-page #(set! js/window.location "/login")]
                (condp = detail
                  invalid-token-msg (reload-page)
                  token-expired-msg (reload-page)
                  login-page))
          (put! response-channel response))))
    response-channel))
