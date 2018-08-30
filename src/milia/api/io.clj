(ns milia.api.io
  (:import [com.fasterxml.jackson.core JsonParseException]
           [java.io File]
           [org.apache.http NoHttpResponseException])
  (:require [cheshire.core :as json]
            [clj-http.client :as client]
            [clj-http.conn-mgr :as conn-mgr]
            [clojure.java.io :as io]
            [clojure.string :refer [join]]
            [clojure.tools.logging :as log]
            [milia.helpers.io :refer [error-status?]]
            [milia.utils.file :as file-utils]
            [milia.utils.remote :refer [*credentials* bad-token-msgs debug-api?
                                        http-default-per-route http-threads
                                        make-url timeouts]]
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

(defn multipart-options
  "Parse file and return multipart options"
  [file name]
  (let [data-file (file-utils/uploaded->file file)]
    {:multipart [{:name name
                  :content data-file}]}))

(defn- req+auth
  "Add authorization to options"
  [http-options]
  (let [{:keys [temp-token token username password]} *credentials*
        {:keys [auth-token]} http-options]
    (if (or temp-token token auth-token)
      (assoc http-options
             :headers {"Authorization"
                       (join
                        " "
                        (if (and temp-token (not auth-token))
                          ["TempToken" temp-token]
                          ["Token" (or auth-token token)]))})
      (merge http-options
             (when password {:digest-auth [username password]})))))

(defn build-req
  ([] (build-req nil))
  ([http-options]
   (assoc (req+auth (or http-options {}))
          :conn-timeout (:conn-timeout @timeouts)
          :socket-timeout (:socket-timeout @timeouts)
          :save-request? debug-api?
          :debug debug-api?
          :debug-body debug-api?)))

(defn debug-api
  "Print out debug information."
  [method url http-options {:keys [status body request] :as response}]
  (when debug-api?
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
  [^String body]
  (try+
   (json/parse-string body true)
   (catch ClassCastException _
     (parse-json-response (String. body "UTF-8")))
   (catch JsonParseException _
     (str "Improperly formatted API response: " body))))

(defn parse-binary-response
  "Parse binary response by writing into a temp file and returning the path."
  [body filename & {:keys [url http-options]}]
  (let [tempfile (File/createTempFile filename "")
        path (str (.getAbsolutePath tempfile))
        ^File file (clojure.java.io/file path)
        ;; Stream the http-request to avoid out of memory errors when the data
        ;; to copy is large
        json-file?
        (when filename (.endsWith filename ".json"))
        {streamed-body :body status :status}
        (when json-file?
          (client/get url
                      (build-req (assoc http-options :as (keyword "stream")))))]
    (.deleteOnExit file)
    ;; io/copy is used since it takes an input-stream and an output-stream
    (if (and json-file? (not (error-status? status)))
      (with-open [out-stream (->> file
                                  io/as-file
                                  io/output-stream)]
        (io/copy streamed-body out-stream))
      (parse-json-response streamed-body))
    ;; Broken out so we can add type hints to avoid reflection
    (when-not json-file?
      (if (instance? String body)
        (let [^String body-string body]
          (with-open [^java.io.Writer w (io/writer file :append false)]
            (.write w body-string)))
        (let [^bytes body-bytes body]
          (with-open [^java.io.OutputStream w (io/output-stream file
                                                                :append
                                                                false)]
            (.write w body-bytes)))))
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
       {;; Maximum number of simultaneous connections per host
        :default-per-route http-default-per-route
        ;; Maximum number of threads that will be used for connecting
        :threads http-threads}
       (try+
        (send-request)
        (catch expired-token? _
          (refresh-temp-token)
          (send-request))
        (catch NoHttpResponseException _
          ;; Because Core doesn't respond with a 401 on unauthorized PATCH
          ;; requests
          (refresh-temp-token)
          (send-request))))
     (catch #(or (nil? (:status %))
                 (<= 400 (:status %))) response
       ;; This deals with secondary error responses that do not match the
       ;; expired-token? criteria
       response))))
