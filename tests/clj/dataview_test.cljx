(ns milia.api.dataview-test
  (:require
    [midje.sweet :refer :all]
    [milia.api.dataview :refer :all]
    [milia.utils.remote :refer [make-j2x-url]]
    [milia.api.http :refer [parse-http]]
    [milia.api.io :refer [make-url multipart-options]]))


(def account {:username "username"})
(def url :url)
(def params {:name "My DataView",
             :xform "https://ona.io/api/v1/forms/12"
             :project  "https://ona.io/api/v1/projects/13"
             :columns ["name" "age" "gender"]
             :query [{:column "age" :filter ">" :value "20"}]})
(def url "http://some-url/")
(def dataview-id 1)

(fact "about create dataview"
      (let [options (merge params {:suppress-40x-exceptions? true})]
        (create account params) => :response
        (provided
          (make-url "dataviews") => url
          (parse-http :post url account options) => :response)))

(fact "about show dataview"
      (let [options (merge params {:suppress-40x-exceptions? true})]
        (show account dataview-id) => :response
        (provided
          (make-url "dataviews" dataview-id) => url
          (parse-http :get url account options) => :response)))

(fact "about get dataview data"
      (let [options (merge params {:suppress-40x-exceptions? true})]
        (data account dataview-id) => :response
        (provided
          (make-url "dataviews" dataview-id "data") => url
          (parse-http :get url account options) => :response)))
