(ns milia.utils.request
  (:require [cljs.core.async :as async :refer [<! put! chan]]
            [milia.utils.remote :refer [bad-token-msgs]])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(defn refresh-token-url [username] (str "/" username "/temp-token"))

(defn request
  "Wraps cljs-http.client/request and redirects if status is 401"
  [request-fn & args]
  (let [response-channel (chan)]
    (go
      (let [original-response-channel (apply request-fn args)
            {:keys [status] :as response} (<! original-response-channel)]
        (if (= status 401)
          (if (in? bad-token-msgs (-> response :body :detail))
            (set! js/window.location js/window.location)
            (set! js/window.location "/login"))
          (put! response-channel response))))
    response-channel))
