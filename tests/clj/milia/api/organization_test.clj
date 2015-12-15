(ns milia.api.organization-test
  (:refer-clojure :exclude [update])
  (:require [midje.sweet :refer :all]
            [milia.api.organization :refer :all]
            [milia.api.http :refer [parse-http]]
            [milia.utils.remote :refer [make-url]]))

(def url :fake-url)
(def username :fake-username)
(def password :fake-password)
(def org-name :fake-org-name)
(def fake-teams [{:organization org-name :name "name"}])
(def org-profile {:org org-name})

(facts "about organizations"
       (fact "should get correct url"
             (all) => :something
             (provided
              (make-url "orgs") => url
              (parse-http :get url) => :something))

       (fact "should pass shared-with param is username given"
             (all username) => :something
             (provided
              (make-url (str "orgs?shared_with=" username)) => url
              (parse-http :get url) => :something)))

(facts "about organization-create"
       (fact "should associate data"
             (create :data) => :something
             (provided
              (make-url "orgs") => url
              (parse-http :post
                          url
                          :http-options {:form-params :data}
                          :suppress-4xx-exceptions? true
                          :as-map? true) => :something)))

(facts "about teams-all"
       (let [base-url (make-url "teams")
             organization-name "some-organization"
             url-with-filter (str base-url "?org=" organization-name)]
         (fact "calls parse-http without org filter when called without an
                argument"
               (teams-all) => :api-response
               (provided
                (parse-http :get base-url) => :api-response))
         (fact "calls parse-http with org filter when an organization-name is
                supplied"
               (teams-all organization-name) => :api-response
               (provided
                (parse-http :get url-with-filter) => :api-response))))

(facts "about teams"
       (fact "should get correct url"
             (teams org-name) => fake-teams
             (provided
              (make-url "teams") => url
              (parse-http :get url) => fake-teams))

       (fact "should filter out internal team"
             (teams org-name) => fake-teams
             (provided
              (teams-all) => fake-teams)))

(facts "about team-info"
       (fact "should get correct url"
             (team-info :fake-orgname :fake-team-id) => :something
             (provided
              (make-url "teams" :fake-orgname :fake-team-id) => url
              (parse-http :get url) => :something)))

(facts "about team-members"
       (fact "should get correct url"
             (team-members :fake-team-id) => :something
             (provided
              (make-url "teams" :fake-team-id "members") => url
              (parse-http :get url) => :something)))

(fact "about create-team"
      (create-team :params) => :something
      (provided
       (make-url "teams") => url
       (parse-http :post url
                   :http-options {:form-params :params}) => :something))

(fact "about add-team-member"
      (add-team-member :fake-orgname
                       :fake-team-id
                       :user) => :something
      (provided
       (make-url "teams" :fake-orgname :fake-team-id "members") => url
       (parse-http :post url :http-options {:form-params :user}) => :something))

(facts "about members"
       (fact "should get correct url"
             (members :fake-orgname) => :something
             (provided
              (make-url "orgs" :fake-orgname "members") => url
              (parse-http :get url) => :something)))

(facts "about add-member"
       (fact "should add a member with default role"
             (add-member :orgname :member) => :something
             (provided
              (make-url "orgs" :orgname "members") => url
              (parse-http :post
                          url
                          :http-options {:form-params
                                         {:username :member :role editor-role}}
                          :suppress-4xx-exceptions? true
                          :as-map? true) => :something)))

(facts "about add-member with assigned role"
       (fact "should add a member"
             (add-member :orgname :member :role) => :something
             (provided
              (make-url "orgs" :orgname "members") => url
              (parse-http :post
                          url
                          :http-options {:form-params
                                         {:username :member :role :role}}
                          :suppress-4xx-exceptions? true
                          :as-map? true) => :something)))

(facts "about remove-member"
       (fact "should remove a member"
             (remove-member :orgname :member nil) => :something
             (provided
              (make-url "orgs" :orgname "members") => url
              (parse-http :delete
                          url
                          :http-options {:query-params
                                         {:username :member}}) => :something))

       (fact "should remove a member from a team"
             (remove-member :orgname :member :team-id) => :something
             (provided
              (make-url "teams" :orgname :team-id "members") => url
              (parse-http :delete url
                          :http-options {:query-params
                                         {:username :member}}) => :something)))

(facts "about single owner"
       (fact "should be false if multiple members in owners team"
             (single-owner? {:name owners-team-name} [:a :b]) => false)

       (fact "should be true if one member in owners team"
             (single-owner? {:name owners-team-name} [:a]) => true))

(fact "should update org settings"
      (let [params {:org org-name :description "test"}
            data {:form-params {:description "test"}
                  :content-type :json}]
        (update params) => org-profile
        (provided
         (make-url "orgs" org-name) => :url
         (parse-http :patch
                     :url
                     :http-options data
                     :as-map? true) => org-profile)))

(fact "should return all members team for an organization."
      (get-team org-name internal-members-team-name)
      => {:teamid 1 :name internal-members-team-name}
      (provided
       (make-url (str "teams?org=" org-name)) => :url
       (parse-http :get :url :suppress-4xx-exceptions? true)
       => [{:teamid 1 :name internal-members-team-name}]))

(fact "should change default_role permissions on a project for a team"
      (share-team :team-id :data) => :updated-team
      (provided
       (make-url "teams" :team-id "share") => :url
       (parse-http :post :url
                   :http-options {:form-params :data}) => :updated-team))

(facts "about can-user-create-project-under-organization?"
       (let [owner "i_own_this"
             manager "i_run_this"
             member "i_mostly_take_orders"
             outsider "i_simply_exist"
             organization {:org "Big Tent"
                           :users [{:user manager
                                    :role "manager"}
                                   {:user owner
                                    :role "owner"}
                                   {:user member
                                    :role "editor"}]}]
         (fact "returns true if user has 'owner' role"
               (can-user-create-project-under-organization? owner organization)
               => true)
         (fact "returns true if user has 'manager' role"
               (can-user-create-project-under-organization? manager
                                                            organization)
               => true)
         (fact "returns false if user has 'editor' role"
               (can-user-create-project-under-organization? member organization)
               => false)
         (fact "returns false if user does not belong to organization"
               (can-user-create-project-under-organization? outsider
                                                            organization))))
