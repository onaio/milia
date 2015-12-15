(ns milia.api.dataview-test
  (:refer-clojure :exclude [get update])
  (:require [midje.sweet :refer :all]
            [milia.api.dataview :refer :all]
            [milia.api.http :refer [parse-http]]
            [milia.api.io :refer [multipart-options]]
            [milia.utils.remote :refer [make-j2x-url make-url]]))

(def url :url)
(def params {:name "My DataView"
             :xform "https://ona.io/api/v1/forms/12"
             :project  "https://ona.io/api/v1/projects/13"
             :columns ["name" "age" "gender"]
             :query [{:column "age" :filter ">" :value "20"}]})
(def dataview-id 1)

(fact "about create dataview"
      (create params) => :response
      (provided
        (make-url "dataviews") => url
        (parse-http :post
                    url
                    :http-options {:form-params params}
                    :suppress-4xx-exceptions? true) => :response))

(fact "about get dataview"
      (get dataview-id) => :response
      (provided
        (make-url "dataviews" dataview-id) => url
        (parse-http :get url :no-cache? nil
                    :suppress-4xx-exceptions? true) => :response))

(fact "about get dataview data"
      (data dataview-id) => :response
      (provided
        (make-url "dataviews" dataview-id "data.json") => url
        (parse-http :get url :suppress-4xx-exceptions?
                    true :raw-response? true) => :response))

(fact "about get dataview's form"
      (form dataview-id) => :response
      (provided
        (make-url "dataviews" dataview-id "form.json") => url
        (parse-http :get url :suppress-4xx-exceptions? true) => :response))

(fact "about count data returned by dataview"
      (let [options {:query-params {:count true}}]
        (count-data dataview-id) => :response
        (provided
          (make-url "dataviews" dataview-id "data") => url
          (parse-http :get url :http-options options
                      :suppress-4xx-exceptions? true) => :response)))

(fact "about all dataviews"
      (all) => :response
      (provided
        (make-url "dataviews") => url
        (parse-http :get url :suppress-4xx-exceptions? true) => :response))

(fact "about update dataview"
      (let [options {:form-params params}]
        (update dataview-id params) => :response
        (provided
          (make-url "dataviews" dataview-id) => url
          (parse-http :put url :http-options options
                      :suppress-4xx-exceptions? true) => :response)))

(fact "about delete dataview"
      (delete dataview-id) => :response
      (provided
        (make-url "dataviews" dataview-id) => url
        (parse-http :delete url) => :response))
