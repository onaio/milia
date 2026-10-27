(ns milia.api.v2.organization-test
  (:refer-clojure :exclude [update])
  (:require [midje.sweet :refer :all]
            [milia.api.v2.organization :refer :all]
            [milia.api.http :refer [parse-http]]
            [milia.utils.remote :refer [make-v2-url]]))

(def url "fake-url")
(def org-name "fake-org-name")

(facts "about all"
       (fact "should list without filters"
             (all) => :something
             (provided
              (make-v2-url "orgs.json") => url
              (parse-http :get url :http-options {:query-params {}})
              => :something))

       (fact "should pass the shared-with and role filters"
             (all :shared-with "bob" :role "manager") => :something
             (provided
              (make-v2-url "orgs.json") => url
              (parse-http :get
                          url
                          :http-options {:query-params {:shared_with "bob"
                                                        :role "manager"}})
              => :something)))

(fact "about create"
      (create :data) => :something
      (provided
       (make-v2-url "orgs.json") => url
       (parse-http :post
                   url
                   :http-options {:form-params :data}
                   :suppress-4xx-exceptions? true
                   :as-map? true) => :something))

(facts "about profile"
       (fact "should get the organization"
             (profile org-name) => :something
             (provided
              (make-v2-url "orgs" (str org-name ".json")) => url
              (parse-http :get url :no-cache? nil) => :something))

       (fact "should not request without an organization"
             (profile "") => nil))

(fact "should retrieve org metadata"
      (retrieve-org-metadata org-name) => :metadata
      (provided
       (profile org-name) => {:metadata :metadata}))

(fact "should update the organization"
      (update {:org org-name :description "test"}) => :something
      (provided
       (make-v2-url "orgs" (str org-name ".json")) => url
       (parse-http :patch
                   url
                   :http-options {:form-params {:description "test"}
                                  :content-type :json}
                   :as-map? true) => :something))

(fact "about members"
      (members org-name) => :something
      (provided
       (make-v2-url "orgs" org-name "members.json") => url
       (parse-http :get url) => :something))

(facts "about add-member"
       (fact "should add a member with the default role"
             (add-member org-name "bob") => :something
             (provided
              (make-v2-url "orgs" org-name "members.json") => url
              (parse-http :post
                          url
                          :http-options {:form-params {:username "bob"
                                                       :role editor-role}
                                         :content-type :json}
                          :suppress-4xx-exceptions? true
                          :as-map? true) => :something))

       (fact "should add a member with the given role"
             (add-member org-name "bob" "owner") => :something
             (provided
              (make-v2-url "orgs" org-name "members.json") => url
              (parse-http :post
                          url
                          :http-options {:form-params {:username "bob"
                                                       :role "owner"}
                                         :content-type :json}
                          :suppress-4xx-exceptions? true
                          :as-map? true) => :something)))

(fact "about change-member-role"
      (change-member-role org-name "bob" "manager") => :something
      (provided
       (make-v2-url "orgs" org-name "members.json") => url
       (parse-http :put
                   url
                   :http-options {:form-params {:username "bob"
                                                :role "manager"}
                                  :content-type :json}
                   :as-map? true) => :something))

(fact "should remove a member by updating them with remove"
      (remove-member org-name "bob") => :something
      (provided
       (make-v2-url "orgs" org-name "members.json") => url
       (parse-http :patch
                   url
                   :http-options {:form-params {:username "bob"
                                                :remove true}
                                  :content-type :json}
                   :as-map? true) => :something))

(facts "about single-owner-member?"
       (fact "should be true with one owner"
             (single-owner-member? org-name) => true
             (provided
              (members org-name) => [{:user "a" :role "owner"}
                                     {:user "b" :role "manager"}]))

       (fact "should be false with several owners"
             (single-owner-member? org-name) => false
             (provided
              (members org-name) => [{:user "a" :role "owner"}
                                     {:user "b" :role "owner"}])))
