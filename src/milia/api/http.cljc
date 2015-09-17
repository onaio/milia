(ns milia.api.http
  (:require [clojure.set :refer [rename-keys]]
            #?@(:clj [[milia.api.io :refer [build-req parse-response
                                            http-request debug-api]]
                      [slingshot.slingshot :refer [throw+]]]
                :cljs [[milia.api.io :refer [build-http-options token->headers
                                             http-request raw-request]]
                       [cljs-hash.md5  :refer [md5]]
                       [cljs-http.client :as http]
                       [cljs.core.async :as async :refer [<!]]]))
  #?(:cljs (:require-macros [cljs.core.async.macros :refer [go]])))

#?(:clj
   (defn- throw-error
     [reason status response]
     (throw+ {:reason reason
              :detail {:status-code status
                       :response response}})))

(defn parse-http
  "Send and parse an HTTP response as JSON.
   Additional arguments modify beavior of parse-http:
   In both: `raw-response?`, `filename`, `http-options`.
   In CLJ: `suppress-4xx-exceptions?`, `as-map?`.
   In CLJS: `accept-header` `callback`, `no-cache?`.
   When a request fails for one of the following reasons, an exception is thrown
   with a map containing a `:reason` key, and an optional `:detail` key
    1. No response: {:reason :no-http-response}
    2. 4xx response: {:reason :http-client-error
                      :detail {:status-code <status-code>
                               :response <parsed-json-from-server>}
    3. 5xx response: {:reason :http-server-error
                      :detail {:response <raw-response>
                               :status-code <status-code>}"
  [method url & {:keys [accept-header callback filename http-options
                        suppress-4xx-exceptions? raw-response? as-map?
                        no-cache? must-revalidate?]}]
  ;; CLJ: synchronous implementation, checks status before returning.
  #?(:clj
     (let [{:keys [body status] :as response} (http-request
                                               method url http-options)
           parsed-response (parse-response body
                                           status
                                           filename
                                           raw-response?)]
       (debug-api method url http-options response)
       ;; Assume that a nil status indicates an exception
       (cond
        (nil? response) (throw+ {:reason :no-http-response})
        (nil? status) (throw+ {:reason :no-http-status})
        (and status
             (>= status 400) (< status 500) (not suppress-4xx-exceptions?))
        (throw-error :http-client-error status parsed-response)
        (>= status 500) (throw-error :http-server-error status body))
       (if as-map?
         (assoc response :body parsed-response) parsed-response))
     ;; CLJS: asynchronous implementation, returns a channel.
     :cljs
     (if filename
       (throw (js/Error. "File downloads auth not supported via JS"))
       (let [request-fn (if raw-response? raw-request http/request)
             headers (token->headers :get-crsftoken? (= method :delete)
                                     :must-revalidate? must-revalidate?
                                     :accept-header accept-header)
             ch (http-request
                 request-fn
                 (merge (build-http-options http-options method no-cache?)
                        {:headers headers :method method :url url}))]
         (if callback (go (-> ch <! callback)) ch)))))
