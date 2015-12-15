(ns milia.api.io-test
  (:require [midje.sweet :refer :all]
            [clj-http.client :as client]
            [milia.api.http :refer [parse-http]]
            [milia.api.io :refer :all]
            [milia.helpers :refer [slingshot-exception]]
            [milia.utils.remote
             :refer [*credentials* make-url token-expired-msg]]
            [environ.core :refer [env]]))

(def body [1,2])
(def raw-body "bla")
(def response {:body body
               :status 200})
(def file [])
(def raw-response {:body raw-body
                   :status 200})
(def raw-response-error (assoc raw-response :status 400))
(def url "http://fake.com")
(def password "password")
(def username "username")
(def account {:password password
              :username username})
(def empty-account {:password password
                    :username username})
(def api-token "api token")
(def temp-token "temp token")
(def options
  {:socket-timeout socket-timeout
   :conn-timeout connection-timeout
   :save-request? (env :debug-api)
   :debug (env :debug-api)
   :debug-body (env :debug-api)})

(defn with-options [m] (merge options m))
(def as-raw {:raw-response? true})
(def as-map {:as-map? true})

(defn options+auth
  [auth]
  (assoc options :headers {"Authorization" auth}))

(facts "about parse-http"
       (fact "should return a file when filename and use-raw-response are
              passed"
             (parse-http :method
                         url
                         :raw-response? true
                         :filename :filename) => (contains file)
             (provided
              (http-request :method url nil)
              => response
              (parse-binary-response body :filename) => file))

       (fact "should handle return map if status code >= 400 and exceptions
              are suppressed"
             (parse-http :method url
                         :raw-response? true
                         :as-map? true
                         :suppress-4xx-exceptions? true)
             => {:body raw-body :status 400}
             (provided
              (http-request :method url nil)
              => raw-response-error))

       (fact "Should throw an exception if API response has 401 HTTP status"
             (parse-http :method url :raw-response? true)
             => (throws Exception #"throw\+.*:status-code 401")
             (provided
              (http-request :method url nil) => {:status 401}))

       (fact "should return raw body as response in map when flag is set"
             (parse-http :method url
                         :raw-response? true
                         :as-map? true)
             => {:body raw-body :status 200}
             (provided
              (http-request :method url nil)
              => raw-response))

       (fact "should pass http-options"
             (let [http-options {:multipart []}]
               (parse-http :method url
                           :http-options http-options
                           :filename :filename
                           :raw-response? true) => (contains file)
               (provided
                (http-request :method
                              url
                              http-options)
                => response
                (parse-binary-response body :filename) => file))))

(facts "about http-request"
       (fact "should add digest if account has password"
             (let [appended-options (assoc options
                                           :digest-auth [username password])]
               (binding [*credentials* account]
                 (http-request :method url nil)) => :response
               (provided
                (call-client-method :method url appended-options)
                => :response)))

       (fact "should add token if exists and temp-token does not exist"
             (binding [*credentials* (assoc account :token api-token)]
               (let [appended-options (assoc
                                       options
                                       :headers {"Authorization"
                                                 (str "Token " api-token)})]
                 (http-request :method url nil) => :response
                 (provided
                  (call-client-method :method url appended-options)
                  => :response))))

       (fact "should add temp token if exists and token exists"
             (binding [*credentials* (assoc account
                                            :token api-token
                                            :temp-token temp-token)]
               (let [appended-options (assoc options :headers
                                             {"Authorization"
                                              (str "TempToken " temp-token)})]
                 (http-request :method url nil) => :response
                 (provided
                  (call-client-method :method
                                      url
                                      appended-options) => :response))))

       (fact "should not refresh on 401 and no temp-token"
             (binding [*credentials* (assoc account
                                            :token api-token)]
               (let [appended-options (options+auth
                                       (str "Token " api-token))
                     exception {:status 401
                                :body (format "{\"detail\": \"%s\"}"
                                              token-expired-msg)}]
                 (http-request :method url nil) => exception
                 (provided
                  (call-client-method :method url appended-options)
                  =throws=> (slingshot-exception exception)))))

       (fact "should refresh temp token on 401 and rethrow if no change"
             (binding [*credentials* (assoc account
                                            :token api-token
                                            :temp-token temp-token)]
               (let [appended-options (options+auth
                                       (str "TempToken " temp-token))
                     exception {:status 401
                                :body (format "{\"detail\": \"%s\"}"
                                              token-expired-msg)}]
                 (http-request :method url nil) => exception
                 (provided
                  (call-client-method :method url appended-options)
                  =throws=> (slingshot-exception exception)
                  (#'milia.api.io/refresh-temp-token) => nil))))

       (fact "should refresh temp token on 401 and succeed on change"
             (binding [*credentials* (assoc account
                                            :token api-token
                                            :temp-token temp-token)]
               (let [appended-options (options+auth
                                       (str "TempToken " temp-token))
                     new-temp-token "refreshed temp token"
                     options-for-refresh (options+auth
                                          (str "Token " api-token))
                     refreshed-options (options+auth
                                        (str "TempToken " new-temp-token))
                     exception {:status 401
                                :body (format "{\"detail\": \"%s\"}"
                                              token-expired-msg)}]
                 (http-request :method url nil) => :response
                 (provided
                  (call-client-method :method url appended-options)
                  =throws=> (slingshot-exception exception)
                  (make-url "user") => :url
                  (client/get :url options-for-refresh)
                  => {:body :body :status :status}
                  (parse-response :body :status nil false)
                  => {:temp_token new-temp-token}
                  (call-client-method :method url refreshed-options)
                  => :response))))

       (fact "should return response on nil status"
             (binding [*credentials* (assoc account
                                            :token api-token)]
               (let [appended-options (options+auth
                                       (str "Token " api-token))
                     exception {:status nil}]
                 (http-request :method url nil) => exception
                 (provided
                  (call-client-method :method url appended-options)
                  =throws=> (slingshot-exception exception)))))

       (fact "should return response on 50x error"
             (binding [*credentials* (assoc account
                                            :token api-token)]
               (let [appended-options (options+auth
                                       (str "Token " api-token))
                     exception {:status 502
                                :body "Server error"}]
                 (http-request :method url nil) => exception
                 (provided
                  (call-client-method :method url appended-options)
                  =throws=> (slingshot-exception exception))))))
