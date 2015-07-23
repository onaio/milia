(ns milia.api.io-test
  (:require [midje.sweet :refer :all]
            [milia.api.http :refer [parse-http]]
            [milia.api.io :refer :all]
            [milia.utils.remote :refer [*credentials*]]
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
   :connection-manager connection-manager
   :save-request? (env :debug-api)
   :debug (env :debug-api)
   :debug-body (env :debug-api)})

(defn with-options [m] (merge options m))
(def as-raw {:raw-response? true})
(def as-map {:as-map? true})
(def suppressed {:suppress-40x-exceptions? true})

(facts "about parse-http"
       (fact "should return a file when filename and use-raw-response are
              passed"
             (parse-http :method
                         url
                         :raw-response? true
                         :filename :filename) => (contains file)
                         (provided
                          (#'milia.api.io/http-request :method url options)
                          => response
                          (parse-binary-response body :filename) => file))

       (fact "should handle return map if status code >= 400 and exceptions
              are suppressed"
             (parse-http :method url
                         :raw-response? true
                         :as-map? true
                         :suppress-40x-exceptions? true)
             => {:body raw-body :status 400}
             (provided
              (#'milia.api.io/http-request :method url options)
              => raw-response-error))

       (fact "Should throw an exception if API response has 401 HTTP status"
             (parse-http :method url :raw-response? true)
             => (throws Exception #"throw\+.*:api-response-status 401")
             (provided
              (#'milia.api.io/http-request :method url options) => {:status 401}))

       (fact "should return raw body as response in map when flag is set"
             (parse-http :method url
                         :raw-response? true
                         :as-map? true)
             => {:body raw-body :status 200}
             (provided
              (#'milia.api.io/http-request :method url options)
              => raw-response))

       (fact "should append to existing options"
             (let [http-options {:multipart [] :raw-response? true}
                   appended-options (merge options http-options)]
               (parse-http :method url
                           :http-options http-options
                           :filename :filename) => (contains file)
                           (provided
                            (#'milia.api.io/http-request :method
                                                         url
                                                         appended-options)
                            => response
                            (parse-binary-response body :filename) => file)))

       (fact "should add digest if account has password"
             (let [appended-options (assoc options
                                           :digest-auth [username password])]
               (binding [*credentials* (atom account)]
                 (parse-http :method url
                             :raw-response? true
                             :filename :filename)) => (contains file)
                           (provided
                            (#'milia.api.io/http-request :method
                                                         url
                                                         appended-options)
                            => response
                            (parse-binary-response body :filename) => file)))

       (fact "should add token if exists and temp-token does not exist"
             (binding [*credentials* (atom (assoc account :token api-token))]
               (let [appended-options (assoc options
                                             :headers {"Authorization"
                                                       (str "Token " api-token)})]
                 (parse-http :method url
                             :raw-response? true
                             :filename :filename) => (contains file)
                             (provided
                              (#'milia.api.io/http-request :method
                                                           url
                                                           appended-options)
                              => response
                              (parse-binary-response body :filename) => file))))

       (fact "should add temp token if exists and token exists"
             (binding [*credentials* (atom (assoc account
                                                  :token api-token
                                                  :temp-token temp-token))]
               (let [appended-options (assoc options :headers
                                             {"Authorization"
                                              (str "TempToken " temp-token)})]
                 (parse-http :method
                             url
                             :raw-response? true
                             :filename :filename) => (contains file)
                             (provided
                              (#'milia.api.io/http-request :method
                                                           url
                                                           appended-options)
                              => response
                              (parse-binary-response body :filename) => file)))))
