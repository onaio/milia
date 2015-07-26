(ns milia.api.http
  (:require [clojure.set :refer [rename-keys]]
            #?@(:clj [[milia.api.io :refer [parse-response http-request
                                            debug-api add-to-options]]
                      [milia.utils.seq :refer [in?]]
                      [slingshot.slingshot :refer [throw+]]]
                :cljs [[milia.api.io :refer [build-http-options token->headers
                                             raw-request]]
                       [cljs-hash.md5  :refer [md5]]
                       [cljs-http.client :as http]
                       [milia.utils.request :refer [request]]
                       [cljs.core.async :as async :refer [<!]]]))
  #?(:cljs (:require-macros [cljs.core.async.macros :refer [go]])))

(defn parse-http
  "Send and parse an HTTP response as JSON.
   Additional arguments modify beavior of parse-http:
   In both: `raw-response?`, `filename`, `http-options`.
   In clj: `suppress-40x-exceptions?`, `as-map?`.
   In cljs: `callback`, `no-cache?`."
  [method url & options]
  (let [{:keys [callback filename http-options suppress-40x-exceptions?
                raw-response? as-map? no-cache? must-revalidate?]} options]
    ;; CLJ: synchronous implementation, checks status before returning.
    ;; callback, no-cache? have no meaning in CLJ a.t.m.
    #?(:clj
       (let [appended-options (add-to-options http-options)
             {:keys [body status]
              :as response} (http-request method url appended-options)
             parsed-response (parse-response body
                                             status
                                             filename
                                             raw-response?)]
         (debug-api method url appended-options response)
         (when (and (in? [400 401 404] status) (not suppress-40x-exceptions?))
           (throw+ {:api-response-status status
                    :parsed-api-response parsed-response}))
         (if as-map?
           (assoc response :body parsed-response)
           parsed-response))
       ;; CLJS: asynchronous implementation, returns a channel.
       ;; suppress-40x-exceptions?, as-map? have no meaning in CLJS a.t.m.
       :cljs
       (if filename
         (throw (js/Error. "File downloads auth not supported via JS"))
         (let [request-fn (if raw-response? raw-request http/request)
               headers (token->headers :get-crsftoken? (= method :delete)
                                       :must-revalidate? must-revalidate?)
               ch (request request-fn
                           (merge (build-http-options
                                   http-options method no-cache?)
                                  {:xhr true
                                   :headers headers
                                   :method method
                                   :url url}))]
           (if callback (go (-> ch <! callback)) ch))))))
