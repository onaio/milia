(ns milia.api.http
  (:require [clojure.set :refer [rename-keys]])
  #?(:clj (:require [milia.api.io :refer [parse-response http-request
                                          debug-api add-to-options]]
                    [milia.utils.seq :refer [in?]]
                    [slingshot.slingshot :refer [throw+]])
     :cljs (:require [milia.api.io :refer [token->headers raw-request]]
                     [cljs-hash.md5  :refer [md5]]
                     [cljs-http.client :as http]
                     [milia.utils.request :refer [request]])))

;;; PARSE HTTP ;;;;;

(defn parse-http
  "Send and parse an HTTP response as JSON.
   Additional arguments modify beavior of parse-http:
   In both: `raw-response?`, `filename`, `http-options`.
   In clj: `suppress-40x-exceptions?`, `as-map?`.
   In cljs: `no-cache?`."
  [method url & options]
  (let [{:keys [filename http-options suppress-40x-exceptions?
                raw-response? as-map? no-cache? must-revalidate?]} options]
    ;; CLJ: synchronous implementation, checks status before returning.
    ;; no-cache? has no meaning in CLJ a.t.m.
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
       (let [http-request (if raw-response? raw-request request)
             headers (token->headers :token auth-token
                                     :get-crsftoken? (= method http/delete)
                                     :must-revalidate? must-revalidate?)]
         (when filename
           (throw (js/Error. "File downloads auth not supported via JS")))
         (http-request (merge (build-http-options http-options method no-cache?)
                              {:xhr true
                               :headers headers
                               :method method
                               :url url}))))))
