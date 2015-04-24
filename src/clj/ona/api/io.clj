(ns ona.api.io
  (:import [com.fasterxml.jackson.core JsonParseException])
  (:require [cheshire.core :as json]
            [clj-http.client :as client]
            [clj-http.conn-mgr :as conn-mgr]
            [clojure.java.io :as io]
            [environ.core :refer [env]]
            [ona.helpers.io :refer [error-status?]]
            [ona.utils.file :as file-utils]
            [ona.utils.remote :as remote]
            [ona.utils.seq :refer [in?]]
            [slingshot.slingshot :refer [throw+ try+]]))

(def ^:private meths
  {:delete client/delete
   :get client/get
   :patch client/patch
   :post client/post
   :put client/put})

(defonce connection-manager
  ;; A connection manager so that we can use persistent connections.
  (conn-mgr/make-reusable-conn-manager
   {
    ;; max simultaneous connections per host
    :default-per-route (env :jetty-min-threads)
    ;; max threads used for connecting
    :threads (env :jetty-min-threads)
    ;; time connections left open in seconds
    :timeout 10}))

;; timeout waiting for data, 5 seconds less than nginx
(def socket-timeout 115000)

;; timeout until a connection is established, 5 seconds less than nginx
(def connection-timeout 55000)

;; do not verify SSL for these hosts
(def ssl-whitelist [remote/thumbor-host])

;; Shadowing CLJX function
(def make-url remote/make-url)

(defn multipart-options
  "Parse file and return multipart options"
  [file name]
  (let [data-file (file-utils/uploaded->file file)]
    {:multipart [{:name name
                  :content data-file}]}))

(defn- whitelisted-for-ssl?
  "If the url is in our whitelist do not verfiy SSL."
  [url]
  (in? ssl-whitelist (.getHost (io/as-url url))))

(defn- add-auth-to-options
  "Add authorization to options"
  [{:keys [username password api_token temp_token]} options]
  (if api_token
    (assoc options
      :headers {"Authorization" (if (:use-temp-token options)
                                  (str "TempToken " temp_token)
                                  (str "Token " api_token))})
    (merge options (when password {:digest-auth [username password]}))))

(defn add-to-options
  [account options url]
  (assoc (add-auth-to-options account options)
    :socket-timeout socket-timeout
    :conn-timeout connection-timeout
    :insecure? (whitelisted-for-ssl? url)
    :connection-manager connection-manager
    :save-request? (env :debug-api?)
    :debug (env :debug-api?)
    :debug-body (env :debug-api?)))

(defn debug-api
  "Print out debug information."
  [method url options {:keys [status body request] :as response}]
  (println "\n-- parse-http output --"
           "\n\n-- REQUEST --"
           "\n-- method: " method
           "\n-- url: " url
           "\n-- options: " options
           "\n\n-- RESPONSE --"
           "\n-- status: " status
           "\n-- body: " body
           "\n-- request: " request
           "\n-- complete response: " response))

(defn http-request
  "Send an HTTP request and catch some exceptions."
  [method url options]
  (try+
   ((meths method) url options)
   ;; cautiously default to a fake 555 status if no status is returned
   (catch #(<= 400 (:status % 555)) response
     response)))

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
                            io/writer io/output-stream)
                          file :append false)]
      (.write out-file body))
    path))

(defn parse-response
  "Parse a response based on status, filename, and flags"
  [body status filename use-raw-response?]
  (if (and filename (not (error-status? status)))
    (parse-binary-response body filename)
    (if use-raw-response?
      body
      (parse-json-response body))))
