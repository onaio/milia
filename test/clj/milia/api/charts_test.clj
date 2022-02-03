(ns milia.api.charts-test
  (:refer-clojure :exclude [get])
  (:require [midje.sweet :refer :all]
            [milia.api.charts :refer :all]
            [milia.api.http :refer [parse-http]]
            [milia.utils.remote :refer [make-url]]))

(let [url :fake-url
      dataset-id :fake-dataset-id
      dataview-id "fake-dataview-id"
      field-name "fake-field-name"
      field-xpath "fake-field-xpath"
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
          (parse-http :get url) => :some-chart))

  (facts "About get chart data"
         (fact "Should get chart data with dataview endpoint")
         (get field-name
              :dataview-id dataview-id
              :dataset-id dataset-id
              :field-xpath field-xpath) => :something
         (provided
          (make-url
           "dataviews/fake-dataview-id/charts.json?field_name=fake-field-xpath")
          => url
          (parse-http :get url) => :something))

  (facts "About get chart data"
         (fact "Should get chart data with field_name param")
         (get field-name
              :dataset-id dataset-id) => :something
         (provided
          (make-url
           "charts/:fake-dataset-id.json?field_name=fake-field-name")
          => url
          (parse-http :get url) => :something))

  (facts "About get chart data"
         (fact "Should get chart data with field_xpath param")
         (get field-name
              :dataset-id dataset-id
              :field-xpath field-xpath) => :something
         (provided
          (make-url
           "charts/:fake-dataset-id.json?field_xpath=fake-field-xpath")
          => url
          (parse-http :get url) => :something)))