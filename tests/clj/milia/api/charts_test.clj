(ns milia.api.charts-test
  (:require [midje.sweet :refer :all]
            [milia.api.charts :refer :all]
            [milia.api.http :refer [parse-http]]
            [milia.utils.remote :refer [make-url]]))

(let [url :fake-url
      dataset-id :fake-dataset-id
      suffix-json (str dataset-id ".json")
      suffix-with-field (str dataset-id ".json?field_name=" :field-name)]

  (facts "about fields"
         "Should get correct url for chart fields"
         (fields dataset-id) => :some-fields
         (provided
          (make-url "charts" suffix-json) => url
          (parse-http :get url) => :some-fields))

  (facts "about chart"
         "Should get correct url for chart"
         (chart dataset-id :field-name) => :some-chart
         (provided
          (make-url "charts" suffix-with-field) => url
          (parse-http :get url) => :some-chart)))
