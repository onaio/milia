(ns ona.api.io-test
  (:require [midje.sweet :refer :all]
            [ona.api.http :refer [parse-http]]
            [ona.api.io :refer :all]
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
(def api-token "api token")
(def temp-token "temp token")
(def options
  {:socket-timeout socket-timeout
   :conn-timeout connection-timeout
   :insecure? false
   :connection-manager connection-manager
   :save-request? (env :debug-api?)
   :debug (env :debug-api?)
   :debug-body (env :debug-api?)})

(defn with-options [m] (merge options m))
(def as-raw {:raw-response? true})
(def as-map {:as-map? true})
(def suppressed {:suppress-40x-exceptions? true})

(def options-r (merge options as-raw))
(def options-rm (merge options as-raw as-map))
(def options-rms (merge options as-raw as-map suppressed))

(facts "about parse-http"
       (fact "should return a file when filename and use-raw-response are
              passed"
             (parse-http :method
                         url
                         {}
                         as-raw
                         :filename) => (contains file)
             (provided
              (#'ona.api.io/http-request :method url options-r)
              => response
              (parse-binary-response body :filename) => file))

       (fact "should handle return map if status code >= 400 and exceptions are suppressed"
             (parse-http :method url {} (merge as-raw as-map suppressed))
             => {:body raw-body :status 400}
             (provided
              (#'ona.api.io/http-request :method url options-rms)
              => raw-response-error))

       (fact "Should throw an exception if API response has 401 HTTP status"
             (parse-http :method url {} as-raw)
             => (throws Exception #"throw\+.*:api-response-status 401")
             (provided
              (#'ona.api.io/http-request :method url options-r) => {:status 401}))

       (fact "should return raw body as response in map when flag is set"
             (parse-http :method url {} (merge as-raw as-map))
             => {:body raw-body :status 200}
             (provided
              (#'ona.api.io/http-request :method url options-rm)
              => raw-response))

       (fact "should append to existing options"
             (let [existing-options {:multipart [] :raw-response? true}
                   appended-options (merge options existing-options)]
               (parse-http :method
                           url
                           {}
                           existing-options
                           :filename) => (contains file)
                           (provided
                            (#'ona.api.io/http-request :method
                                                       url
                                                       appended-options)
                            => response
                            (parse-binary-response body :filename) => file)))

       (fact "should add digest if account has password"
             (let [appended-options (assoc options
                                      :raw-response? true
                                      :digest-auth [username password])]
               (parse-http :method
                           url
                           account
                           as-raw
                           :filename) => (contains file)
                           (provided
                            (#'ona.api.io/http-request :method
                                                       url
                                                       appended-options)
                            => response
                            (parse-binary-response body :filename) => file)))

       (fact "should add token if exists and use-temp-token false"
             (let [account+token (assoc account :api_token api-token)
                   appended-options (assoc options
                                      :raw-response? true
                                      :headers {"Authorization"
                                                (str "Token " api-token)})]
               (parse-http :method
                           url
                           account+token
                           {:raw-response? true}
                           :filename) => (contains file)
               (provided
                (#'ona.api.io/http-request :method
                                           url
                                           appended-options)
                => response
                (parse-binary-response body :filename) => file)))

       (fact "should add temp token if exists and use-temp-token true"
             (let [options+temp (assoc options
                                  :use-temp-token true
                                  :raw-response? true)
                   account+token (assoc account
                                   :api_token api-token
                                   :temp_token temp-token)
                   appended-options (assoc options+temp :headers
                                           {"Authorization"
                                            (str "TempToken " temp-token)})]
               (parse-http :method
                           url
                           account+token
                           options+temp
                           :filename) => (contains file)
               (provided
                (#'ona.api.io/http-request :method
                                           url
                                           appended-options)
                => response
                (parse-binary-response body :filename) => file))))
