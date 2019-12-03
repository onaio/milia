(ns milia.utils.images-test
  (:require [midje.sweet :refer :all]
            [milia.utils.images :refer :all]))

(facts "about resize-image"
        (fact "should return with image-url and edge dimension"
                (resize-image "s3.aws.org" 80) =>
                "https://images.ona.io/unsafe/80x80/smart/s3.aws.org")

        (fact "should return with image-url, width and height dimension"
                (resize-image "s3.aws.org" 80 56)
                => "https://images.ona.io/unsafe/80x56/smart/s3.aws.org")

        (fact "should return with image-url, width and height dimension as well
                as optional image-server-url"
                (resize-image "s3.aws.org" 80 56 "https://images.test.org")
                => "https://images.test.org/unsafe/80x56/smart/s3.aws.org"))