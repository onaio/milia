(ns milia.api.j2x-test
  (:require [cheshire.core :refer [parse-string]]
            [midje.sweet :refer :all]
            [milia.api.http :refer [parse-http]]
            [milia.api.j2x :as j2x]
            [milia.utils.file :as f]
            [milia.utils.remote :as r]))

(facts "about upload-xls-template"
       "Test upload xls response"
       (let [upload-response {:status 201
                              :body "uuid"}
             url (r/make-j2x-url "templates")]
         (j2x/upload-xls-template :media-file) => (contains upload-response)
         (provided
          (f/uploaded->file :media-file) => :fake-file
          (f/to-byte-array :fake-file) => :fake-bytearray
          (parse-http :post
                      url
                      :http-options {:body :fake-bytearray}
                      :raw-response? true
                      :suppress-4xx-exceptions? true
                      :as-map? true) => upload-response)))
(facts "about downloading xls-report-templates"
       "Should download xls report template"
       (j2x/download-xls-report-template :filename
                                         :template-token) => :byte-array
       (provided
        (r/make-j2x-url "templates" :template-token) => :url
        (parse-http :get
                    :url
                    :http-options {:as :byte-array}
                    :as-map? true
                    :filename :filename) => :byte-array))
