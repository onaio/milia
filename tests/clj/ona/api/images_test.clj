(ns ona.api.images-test
  (:require [ona.api.images :refer :all]
            [ona.utils.remote :refer [thumbor-server]]
            [ona.api.http :refer [parse-http]]
            [ona.api.io :refer [multipart-options]]
            [midje.sweet :refer :all]))

(def image-map {:filename :filename :size 1})
(def multipart-options-map {:multipart []
                            :raw-response? true
                            :suppress-40x-exceptions? true
                            :as-map? true})
(def merged-options (assoc multipart-options-map
                      :headers {"Slug" :filename}))
(def location-leading-slash "/location")
(def location-url (str thumbor-server location-leading-slash))

(facts "about upload"
       (fact "should return nil if file is nil"
             (upload nil) => nil)

       (fact "should return nil if size is missing"
             (upload {}) => nil)

       (fact "should return nil if size is 0"
             (upload {:size 0}) => nil)

       (fact "should call parse-http if file is not nil"
             (upload image-map) =>  location-url
             (provided
              (multipart-options image-map "media") => multipart-options-map
              (parse-http :post upload-url nil merged-options nil)
              => {:status 200 :headers {"Location" location-leading-slash}})))
