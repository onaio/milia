(ns ona.api.charts-test
  (:require [midje.sweet :refer :all]
            [ona.api.charts :refer :all]
            [ona.api.http :refer [parse-http]]
            [ona.api.io :refer [make-url]]))

(let [url :fake-url
      account :fake-account
      dataset-id :fake-dataset-id
      suffix-json (str dataset-id ".json")
      suffix-with-field (str dataset-id ".json?field_name=" :field-name)]

  (facts "about fields"
         "Should get correct url for chart fields"
         (fields account dataset-id) => :some-fields
         (provided
          (make-url "charts" suffix-json) => url
          (parse-http :get url account) => :some-fields))

  (facts "about chart"
         "Should get correct url for chart"
         (chart account dataset-id :field-name) => :some-chart
         (provided
          (make-url "charts" suffix-with-field) => url
          (parse-http :get url account) => :some-chart)))
