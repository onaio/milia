(ns milia.api.http-test
  (:require [midje.sweet :refer :all]
            [milia.api.http :refer [parse-http]]
            [milia.api.io :refer [debug-api http-request parse-response]]
            [milia.helpers :refer [slingshot-exception]]))

(def http-4xx-codes
  "Valid HTTP 4xx codes
   Source: https://en.wikipedia.org/wiki/List_of_HTTP_status_codes#4xx_Client_Error"
  [400 401 402 403 404 405 406 407 408 409 410 411 412 413 414 415 416 417 418
   419 420 420 421 422 423 424 426 428 429 431 440 444 449 450 451 451 494 495
   496 497 498 499 499])

(def http-5xx-codes
  "Valid HTTP 5xx codes
   Source: https://en.wikipedia.org/wiki/List_of_HTTP_status_codes#5xx_Client_Error"
  [500 501 502 503 504 505 506 507 508 509 510 511 520 522 598 599])

(facts "about parse-http"
  (fact "throws an exception when the API server is not reachable"
    (parse-http :method :url)
    => (throws "throw+: {:reason :no-http-response}")
    (provided
     (http-request :method :url nil) => nil
     (parse-response nil nil nil nil) => nil))
  (fact "throws an exception when the API server returns a 4xx"
    (let [status-code (rand-nth http-4xx-codes)]
      (parse-http :method :url)
      => (throws "throw+: {:reason :http-client-error, :detail {:api-response-status 401, :parsed-api-response nil}}")
      (provided
       (http-request :method :url nil) => {:body :body :status 401}
       (parse-response :body 401 nil nil) => nil)))
  (fact "throws an exception when the API server returns a 5xx"
    (let [status-code (rand-nth http-5xx-codes)]
      (parse-http :method :url)
      => (throws (str "throw+: {:reason :http-server-error, :detail {:response {:body :something-nasty, :status "
                               status-code
                               "}}}"))
      (provided
       (http-request :method :url nil) => {:body :something-nasty :status status-code}
       (parse-response :something-nasty status-code nil nil) => nil))))
