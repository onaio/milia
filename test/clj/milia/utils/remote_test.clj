(ns milia.utils.remote-test
  (:require [midje.sweet :refer :all]
            [milia.utils.remote :refer :all]))

(binding [hosts (atom @hosts)]
  (facts "about set-hosts"
         (fact "should always swap in data-host"
               (set-hosts :data-host) => (assoc  @hosts
                                                 :data :data-host))

         (fact "should ignore passed nils"
               (set-hosts :data-host nil nil nil)
               => (merge {:data :data-host} @hosts))

         (fact "should only ignore passed nils"
               (set-hosts :data-host nil :j2x-host nil)
               => (assoc @hosts :data :data-host :j2x :j2x-host))

         (fact "should set all args"
               (set-hosts :data-host :client-host :j2x-host :req-proto)
               => (assoc @hosts
                         :data :data-host
                         :client :client-host
                         :j2x :j2x-host
                         :request-protocol :req-proto))))

(binding [*credentials* {}]
  (facts "about set-credentials"
         (fact "should set username and others to nil"
               (set-credentials :username) => {:username :username
                                               :password nil
                                               :temp-token nil
                                               :token nil})

         (fact "should set passed args"
               (set-credentials :username nil :temp-token)
               => {:username :username
                   :password nil
                   :temp-token :temp-token
                   :token nil})

         (fact "should set all passed args"
               (set-credentials :username :password :temp-token :token)
               => {:username :username
                   :password :password
                   :temp-token :temp-token
                   :token :token})))

(def example-hosts
  {:client "client.example.org"
   :data "api.example.org"
   :j2x "j2x.example.org"
   :images "images.example.org"
   :request-protocol "https"})

(defn- host-key-of
  [host-key]
  #(= {:host-key host-key} (ex-data %)))

(facts "about hosts"
       (fact "is dynamic with an atom as its root binding"
             (-> #'hosts meta :dynamic) => true
             (instance? clojure.lang.Atom hosts) => true)

       (fact "URL builders read the bound atom at call time"
             (binding [hosts (atom example-hosts)]
               [(make-url "forms")
                (make-json-url "forms")
                (make-client-url "bob")
                (make-j2x-url "templates")])
             => ["https://api.example.org/api/v1/forms"
                 "https://api.example.org/api/v1/forms.json"
                 "https://client.example.org/bob"
                 "https://j2x.example.org/templates"])

       (fact "a binding does not change the root atom"
             (binding [hosts (atom example-hosts)] (make-url "forms"))
             (:data @hosts) =not=> "api.example.org"))

(facts "about unconfigured hosts"
       (fact "a nil data host throws with its key"
             (binding [hosts (atom (assoc example-hosts :data nil))]
               (make-url "forms"))
             => (throws clojure.lang.ExceptionInfo
                        "No host configured for :data"
                        (host-key-of :data)))

       (fact "make-json-url goes through the same check"
             (binding [hosts (atom (assoc example-hosts :data nil))]
               (make-json-url "forms"))
             => (throws clojure.lang.ExceptionInfo (host-key-of :data)))

       (fact "a blank data host throws like a nil one"
             (binding [hosts (atom (assoc example-hosts :data ""))]
               (make-url "forms"))
             => (throws clojure.lang.ExceptionInfo (host-key-of :data)))

       (fact "a nil client host throws with its key"
             (binding [hosts (atom (dissoc example-hosts :client))]
               (make-client-url "bob"))
             => (throws clojure.lang.ExceptionInfo (host-key-of :client)))

       (fact "a nil j2x host throws with its key"
             (binding [hosts (atom (assoc example-hosts :j2x nil))]
               (make-j2x-url "templates"))
             => (throws clojure.lang.ExceptionInfo (host-key-of :j2x)))

       (fact "a nil request protocol throws with its key"
             (binding [hosts (atom (assoc example-hosts
                                          :request-protocol nil))]
               (make-url "forms"))
             => (throws clojure.lang.ExceptionInfo
                        (host-key-of :request-protocol)))

       (fact "protocol-prefixed still accepts a nil resource"
             (binding [hosts (atom example-hosts)]
               (protocol-prefixed nil))
             => "https://"))

(facts "about thumbor-server"
       (fact "follows the bound images host"
             (binding [hosts (atom example-hosts)] (thumbor-server))
             => "https://images.example.org")

       (fact "throws when no images host is configured"
             (binding [hosts (atom (assoc example-hosts :images nil))]
               (thumbor-server))
             => (throws clojure.lang.ExceptionInfo (host-key-of :images))))
