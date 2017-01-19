(ns milia.utils.retry-test
  (:require [chimera.seq :refer [mapply]]
            [midje.sweet :refer :all]
            [milia.utils.retry :refer :all]
            [milia.api.http :refer [parse-http]]))

(facts "about retry-parse-http"
       (fact "should return result of mapply parse-http"
             (retry-parse-http :method :url) => :response
             (provided
              (mapply parse-http :method :url nil) => :response))

       (doseq [status default-retry-for-statuses]
         (fact "should retry for status in retry statuses"
               (retry-parse-http :method :url) => {:status status}
               (provided
                (mapply parse-http :method :url nil)
                => {:status status} :times 2))))
