(ns milia.api.http
  #+clj (:require [milia.api.io :refer [parse-response http-request
                                      debug-api add-to-options ]]
                  [environ.core :refer [env]]
                  [milia.utils.seq :refer [in?]]
                  [slingshot.slingshot :refer [throw+ try+]])
  #+cljs (:require [milia.api.io :refer [token->headers raw-request]]
                   [cljs-hash.md5  :refer [md5]]
                   [cljs-http.client :as http]))

;;; PARSE HTTP ;;;;;

(defn parse-http
  "Send and parse an HTTP response as JSON.
   Options object has special keys that modify beavior of parse-http:
   In clj: `suppress-40x-exceptions?`, `as-map?`,`raw-response?`.
   In cljs: `raw-response?`, `no-cache?`."
  ([method url account]
   (parse-http method url account {}))
  ([method url account options]
   (parse-http method url account options nil))
  ([method url account options filename]
   (let [{:keys [suppress-40x-exceptions? raw-response? as-map?
                 no-cache? must-revalidate?]} options]
     ;; CLJ: synchronous implementation, checks status before returning.
     ;; no-cache? has no meaning in clj a.t.m.
     #+clj
     (let [appended-options (add-to-options account options url)
           {:keys [body status]
            :as response} (http-request method url appended-options)
           parsed-response (parse-response body
                                           status
                                           filename
                                           raw-response?)]
       (when (env :debug-api?)
         (debug-api method url appended-options response))
       (when (and (in? [400 401 404] status) (not suppress-40x-exceptions?))
         (throw+ {:api-response-status status :parsed-api-response parsed-response}))
       (if as-map?
         (assoc response :body parsed-response)
         parsed-response))
     ;; CLJS: asynchronous implementation, returns a channel.
     ;; suppress-40x-exceptions?, as-map? have no meaning in cljs a.t.m.
     #+cljs
     (let [auth-token account ; in cljs, we just get the auth-token, not full account
           http-request (if raw-response? raw-request http/request) ;; v0
           ;; For :post / :put / :patch, we need :form-params, for the rest :query-params
           options (if-not (contains? #{:post :put :patch} method)
                     (assoc options :query-params (:form-params options))
                     options)
           headers (token->headers :token auth-token
                                   :get-crsftoken? (= method http/delete)
                                   :must-revalidate? must-revalidate?)
           time-params (when no-cache?
                         {:t (md5 (.toString (.now js/Date)))})
           all-params (merge options
                             {:xhr true
                              :headers headers
                              :method method
                              :url url})]
       (when filename
         (throw (js/Error. "File downloads auth not supported via js")))
       (http-request all-params)))))