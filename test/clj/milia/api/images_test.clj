(ns milia.api.images-test
  (:require [milia.api.images :refer :all]
            [milia.utils.remote :refer [thumbor-server]]
            [milia.api.http :refer [parse-http]]
            [milia.api.io :refer [multipart-options]]
            [midje.sweet :refer :all]))

(def image-map {:filename :filename :size 1})
(def multipart-options-map {:multipart []})
(def merged-options (assoc multipart-options-map
                           :headers {"Slug" :filename}))
(def location-leading-slash "/location")
(def location-url (str thumbor-server location-leading-slash))
(def image-server-url "http://test.image.com")
(def custom-upload-url (or (and image-server-url
                                (str image-server-url "/image"))
                           upload-url))

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
              (parse-http :post upload-url
                          :http-options  merged-options
                          :as-map? true
                          :suppress-4xx-exceptions? true)
              => {:status 201 :headers {"Location" location-leading-slash}}))

       (fact "should call parse-http if file is not nil and optional image url
              is passed as an arg"
             (upload image-map image-server-url) =>
             (str image-server-url location-leading-slash)
             (provided
              (multipart-options image-map "media") => multipart-options-map
              (parse-http :post custom-upload-url
                          :http-options  merged-options
                          :as-map? true
                          :suppress-4xx-exceptions? true)
              => {:status 201 :headers {"Location" location-leading-slash}})))
