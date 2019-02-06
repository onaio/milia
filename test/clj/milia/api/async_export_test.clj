(ns milia.api.async-export-test
  (:require [midje.sweet :refer :all]
            [milia.api.async-export :refer :all]
            [milia.api.http :refer [parse-http]]
            [milia.utils.remote :refer [make-url *credentials*]]))

(fact "get-exports-per-form calls parse-http with the correct parameters"
      (let [url (str "export.json?xform="
                     :dataset-id
                     "&temp_token="
                     :my-temp-token)]
        (binding [*credentials* {:temp-token :my-temp-token}]
          (get-exports-per-form :dataset-id) => :api-response
          (provided
           (make-url url) => :url
           (parse-http :get :url) => :api-response))))

(fact "delete-export calls parse-http with the correct host url"
      (let [url (str :export-id "?temp_token=" :my-temp-token)]
        (binding [*credentials* {:temp-token :my-temp-token}]
          (delete-export :export-id) => :api-response
          (provided
           (make-url "export" url)
           => :url
           (parse-http :delete :url) => :api-response))))
