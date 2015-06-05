(ns milia.api.dataview-test
  (:require [midje.sweet :refer :all]
            [milia.api.dataview :refer :all]
            [milia.utils.remote :refer [make-j2x-url]]
            [milia.api.http :refer [parse-http]]
            [milia.api.io :refer [make-url multipart-options]]))


(def account {:username "username"})
(def url :url)
(def params {:name "My DataView"
             :xform "https://ona.io/api/v1/forms/12"
             :project  "https://ona.io/api/v1/projects/13"
             :columns ["name" "age" "gender"]
             :query [{:column "age" :filter ">" :value "20"}]})
(def options {:suppress-40x-exceptions? true})
(def dataview-id 1)

(fact "about create dataview"
      (let [options (assoc options :form-params params)]
        (create account params) => :response
        (provided
          (make-url "dataviews") => url
          (parse-http :post url account options) => :response)))

(fact "about show dataview"
      (show account dataview-id) => :response
      (provided
        (make-url "dataviews" dataview-id) => url
        (parse-http :get url account options) => :response))

(fact "about get dataview data"
      (let [options (assoc options :raw-response? true)]
        (data account dataview-id) => :response
        (provided
          (make-url "dataviews" dataview-id "data.json") => url
          (parse-http :get url account options) => :response)))

(fact "about count data returned by dataview"
      (let [options (assoc options :query-params {:count true})]
        (count-data account dataview-id) => :response
        (provided
          (make-url "dataviews" dataview-id "data") => url
          (parse-http :get url account options) => :response)))

(fact "about all dataviews"
      (all account) => :response
      (provided
        (make-url "dataviews") => url
        (parse-http :get url account options) => :response))

(fact "about update dataview"
      (let [options (assoc options :form-params params)]
        (update account dataview-id params) => :response
        (provided
          (make-url "dataviews" dataview-id) => url
          (parse-http :patch url account options) => :response)))

(fact "about delete dataview"
      (delete account dataview-id) => :response
      (provided
        (make-url "dataviews" dataview-id) => url
        (parse-http :delete url account) => :response))
