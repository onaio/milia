(ns milia.api.io
  (:import [com.fasterxml.jackson.core JsonParseException]
           [org.apache.http NoHttpResponseException])
  (:require [cheshire.core :as json]
            [clj-http.client :as client]
            [clj-http.conn-mgr :as conn-mgr]
            [clojure.java.io :as io]
            [clojure.tools.logging :as log]
            [environ.core :refer [env]]
            [milia.helpers.io :refer [error-status?]]
            [milia.utils.file :as file-utils]
            [milia.utils.remote :refer [*credentials* bad-token-msgs make-url]]
            [milia.utils.seq :refer [in?]]
            [slingshot.slingshot :refer [throw+ try+]]))

(def ^:private client-methods
  {:delete client/delete
   :get client/get
   :patch client/patch
   :post client/post
   :put client/put})

(defn call-client-method
  [method url req]
  ((client-methods method) url req))

;; timeout waiting for data, 5 seconds less than nginx
(def socket-timeout 115000)

;; timeout until a connection is established, 5 seconds less than nginx
(def connection-timeout 55000)

(defn multipart-options
  "Parse file and return multipart options"
  [file name]
  (let [data-file (file-utils/uploaded->file file)]
    {:multipart [{:name name
                  :content data-file}]}))

(defn- req+auth
  "Add authorization to options"
  [req]
  (let [{:keys [temp-token token username password]} *credentials*]
    (if (or temp-token token)
      (assoc req
             :headers {"Authorization" (if temp-token
                                         (str "TempToken " temp-token)
                                         (str "Token " token))})
      (merge req (when password {:digest-auth [username password]})))))

(defn build-req
  ([] (build-req nil))
  ([req]
   (assoc (req+auth (or req {}))
     :conn-timeout connection-timeout
     :socket-timeout socket-timeout
     :save-request? (env :debug-api)
     :debug (env :debug-api)
     :debug-body (env :debug-api))))

(defn debug-api
  "Print out debug information."
  [method url http-options {:keys [status body request] :as response}]
  (when (env :debug-api)
    (log/info (str "-- DEBUG API --"
                   "\nREQUEST"
                   "\n-- method: " method
                   "\n-- url: " url
                   "\n-- http-options: " http-options
                   "\n\nRESPONSE"
                   "\n-- status: " status
                   "\n-- body: " body
                   "\n-- request: " request
                   "\n-- complete response: " response))))

(defn parse-json-response
  "Parse a body as JSON catching formatting exceptions."
  [body]
  (try+
   (json/parse-string body true)
   (catch ClassCastException _
     (parse-json-response (String. body)))
   (catch JsonParseException _
       "Improperly formatted API response: " body)))

(defn parse-binary-response
  "Parse binary response by writing into a temp file and returning the path."
  [body filename]
  (let [tempfile (java.io.File/createTempFile filename "")
        path (str (.getAbsolutePath tempfile))
        file (clojure.java.io/file path)]
    (.deleteOnExit file)
    (with-open [out-file ((if (instance? String body)
                            io/writer io/output-stream) file :append false)]
      (.write out-file body))
    path))

(defn parse-response
  "Parse a response based on status, filename, and flags"
  [body status filename raw-response?]
  (if (and filename (not (error-status? status)))
    (parse-binary-response body filename)
    (if raw-response? body (parse-json-response body))))

(defn- fetch-user-with-token
  "Bind credentials so only the token is set and then fetch the user."
  []
  (binding
      [*credentials* (select-keys *credentials* [:token])]
    (client/get (make-url "user") (build-req))))

(defn- refresh-temp-token
  "Fetch the user credentials using the token credential and replace the stored
   temp-token with the temporary token from the response."
  []
  (let [{:keys [body status]} (fetch-user-with-token)
        {:keys [temp_token]} (parse-response body status nil false)]
    (set! *credentials* (assoc *credentials*
                               :temp-token temp_token))))

(defn- expired-token?
  "Assume any 401s that were requested with a temporary token are the result
   of an expired token."
  [{:keys [status]}]
  (and (= 401 status) (:temp-token *credentials*)))

(defn http-request
  "Send HTTP request and handle exceptions"
  [method url http-options]
  (let [send-request
        #(call-client-method method url (build-req http-options))]
    (try+
     (client/with-connection-pool
       {:default-per-route (env :jetty-min-threads)
        :threads (env :jetty-min-threads)}
       (try+
        (send-request)
        (catch #(expired-token? %) response
          (refresh-temp-token)
          (send-request))
        (catch NoHttpResponseException _
          ;; Because Core doesn't respond with a 401 on unauthorized PATCH requests
          (refresh-temp-token)
          (send-request))))
     (catch #(or (nil? (:status %))
                 (<= 400 (:status %))) response
       ;; This deals with secondary error responses that do not match the
       ;; expired-token? criteria
       response))))
