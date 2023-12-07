(ns milia.utils.images-test
  #_{:clj-kondo/ignore [:refer-all]}
  (:require [midje.sweet :refer :all]
            [milia.utils.images :refer [resize-image]]))

(facts
 "about resize-image"
 (fact "should return with image-url and edge dimension"
       (resize-image "s3.aws.org" 80) =>
       "https://images.ona.io/unsafe/80x80/smart/s3.aws.org")

 (fact "should return with image-url, width and height dimension"
       (resize-image "s3.aws.org" 80 56)
       => "https://images.ona.io/unsafe/80x56/smart/s3.aws.org")

 (fact "should return with image-url, width and height dimension as well
              as optional image-server-url"
       (resize-image "s3.aws.org" 80 56 "https://images.test.org")
       => "https://images.test.org/unsafe/80x56/smart/s3.aws.org")
 (fact
  "Works with thumbor 7.5.0"
  (resize-image
   "https://images.test.org/image/some-hash/image-name.jpg"
   80
   56
   "https://images.test.org")
  => "https://images.test.org/unsafe/80x56/smart/some-hash/image-name.jpg"))
