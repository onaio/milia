(ns milia.utils.remote-test
  (:require-macros [cljs.test :refer (is deftest testing)])
  (:require [cljs.test :as t]
            [milia.utils.remote :as remote]))

(def example-hosts
  {:client "client.example.org"
   :data "api.example.org"
   :j2x "j2x.example.org"
   :images "images.example.org"
   :request-protocol "https"})

(deftest set-hosts-images
  (binding [remote/hosts (atom example-hosts)]
    (testing "the fifth argument sets the images host"
      (remote/set-hosts "api.other.org" nil nil nil "images.other.org")
      (is (= "images.other.org" (:images @remote/hosts)))
      (is (= "https://images.other.org" (remote/thumbor-server))))
    (testing "a nil fifth argument keeps it"
      (remote/set-hosts "api.other.org" nil nil nil nil)
      (is (= "images.other.org" (:images @remote/hosts))))))

(deftest missing-host
  (testing "a configured data host builds a URL"
    (binding [remote/hosts (atom example-hosts)]
      (is (= "https://api.example.org/api/v1/forms"
             (remote/make-url "forms")))))
  (testing "a nil data host throws with its key"
    (binding [remote/hosts (atom (assoc example-hosts :data nil))]
      (is (= {:host-key :data}
             (try (remote/make-url "forms")
                  nil
                  (catch :default e (ex-data e))))))))
