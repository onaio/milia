(ns milia.api.images-test
  (:require [milia.api.images :refer :all]
            [milia.utils.remote :refer [hosts]]
            [milia.api.http :refer [parse-http]]
            [milia.api.io :refer [multipart-options]]
            [midje.sweet :refer :all]))

(def image-map {:filename :filename :size 1})
(def multipart-options-map {:multipart []})
(def merged-options (assoc multipart-options-map
                           :headers {"Slug" :filename}))
(def location-leading-slash "/location")
(def image-server-url "http://test.image.com")
(def custom-upload-url (str image-server-url "/image"))
(def example-hosts
  {:client "client.example.org"
   :data "api.example.org"
   :j2x "j2x.example.org"
   :images "images.example.org"
   :request-protocol "https"})
(def no-images-hosts (assoc example-hosts :images nil))

(facts "about upload-url"
       (fact "follows the bound images host"
             (binding [hosts (atom example-hosts)] (upload-url))
             => "https://images.example.org/image")

       (fact "throws when no images host is configured"
             (binding [hosts (atom no-images-hosts)] (upload-url))
             => (throws clojure.lang.ExceptionInfo
                        #(= {:host-key :images} (ex-data %)))))

(facts "about upload"
       (fact "should return nil if file is nil"
             (upload nil) => nil)

       (fact "should return nil if size is missing"
             (upload {}) => nil)

       (fact "should return nil if size is 0"
             (upload {:size 0}) => nil)

       (fact "should post to the bound images host if file is not nil"
             (binding [hosts (atom example-hosts)] (upload image-map))
             => "https://images.example.org/location"
             (provided
              (multipart-options image-map "media") => multipart-options-map
              (parse-http :post "https://images.example.org/image"
                          :http-options merged-options
                          :as-map? true
                          :suppress-4xx-exceptions? true)
              => {:status 201 :headers {"Location" location-leading-slash}}))

       (fact "should call parse-http if file is not nil and optional image url
              is passed as an arg, even with no images host configured"
             (binding [hosts (atom no-images-hosts)]
               (upload image-map image-server-url))
             => (str image-server-url location-leading-slash)
             (provided
              (multipart-options image-map "media") => multipart-options-map
              (parse-http :post custom-upload-url
                          :http-options merged-options
                          :as-map? true
                          :suppress-4xx-exceptions? true)
              => {:status 201 :headers {"Location" location-leading-slash}})))
