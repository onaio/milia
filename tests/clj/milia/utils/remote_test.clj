(ns milia.utils.remote-test
  (:require [midje.sweet :refer :all]
            [milia.utils.remote :refer :all]))

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
                       :request-protocol :req-proto)))

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
