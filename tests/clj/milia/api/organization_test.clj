(ns milia.api.organization_test
  (:require [midje.sweet :refer :all]
            [milia.api.organization :refer :all]
            [milia.api.http :refer [parse-http]]
            [milia.api.io :refer [make-url]]))

(def url :fake-url)
(def username :fake-username)
(def password :fake-password)
(def org-name :fake-org-name)
(def account {:username username :password password})
(def fake-teams [{:organization org-name :name "name"}])
(def org-profile {:org org-name})

(facts "about organizations"
       "should get correct url"
       (all account) => :something
       (provided
        (make-url "orgs") => url
        (parse-http :get url account) => :something))

(facts "about organization-create"
       "Should associate data"
       (create account :data) => :something
       (provided
        (make-url "orgs") => url
        (parse-http :post
                    url
                    account
                    {:form-params :data}) => :something))

(facts "about teams"
       "should get correct url"
       (teams account org-name) => fake-teams
       (provided
        (make-url "teams") => url
        (parse-http :get url account) => fake-teams)

       "should filter out internal team"
       (teams account org-name) => fake-teams
       (provided
        (teams-all account) => fake-teams))

(facts "about team-info"
       "should get correct url"
       (team-info account :fake-orgname :fake-team-id) => :something
       (provided
        (make-url "teams" :fake-orgname :fake-team-id) => url
        (parse-http :get url account) => :something))

(facts "about team-members"
       "should get correct url"
       (team-members account :fake-team-id) => :something
       (provided
        (make-url "teams" :fake-team-id "members") => url
        (parse-http :get url account) => :something))

(facts "about create-team"
       (create-team account :params) => :something
       (provided
        (make-url "teams") => url
        (parse-http :post url account {:form-params :params}) => :something))

(facts "about add-team-member"
       (add-team-member  account :fake-orgname :fake-team-id :user) => :something
       (provided
        (make-url "teams" :fake-orgname :fake-team-id "members") => url
        (parse-http :post url account {:form-params :user}) => :something))

(facts "about members"
       "should get correct url"
       (members account :fake-orgname) => :something
       (provided
        (make-url "orgs" :fake-orgname "members") => url
        (parse-http :get url account) => :something))

(facts "about add-member"
       "should add a member with default role"
       (add-member account :orgname :member) => :something
       (provided
        (make-url "orgs" :orgname "members") => url
        (parse-http :post
                    url
                    account
                    {:form-params {:username :member :role editor-role}}) => :something))

(facts "about add-member with assigned role"
       "should add a member"
       (add-member account :orgname :member :role) => :something
       (provided
         (make-url "orgs" :orgname "members") => url
         (parse-http :post
                     url
                     account
                     {:form-params {:username :member :role :role}}) => :something))

(facts "about remove-member"
       "should remove a member"
       (remove-member account :orgname :member nil) => :something
       (provided
        (make-url "orgs" :orgname "members") => url
        (parse-http :delete
                    url
                    account
                    {:query-params {:username :member}}) => :something)

       "should remove a member from a team"
       (remove-member account :orgname :member :team-id) => :something
       (provided
        (make-url "teams" :orgname :team-id "members") => url
        (parse-http :delete
                    url
                    account
                    {:query-params {:username :member}}) => :something))

(facts "about single owner"
       "should be false if multiple members in owners team"
       (single-owner? account :orgname :team-id) => false
       (provided
        (team-info account :orgname :team-id) => {:name owners-team-name}
        (team-members account :orgname :team-id) => [username username])

       "should be true if one member in owners team"
       (single-owner? account :orgname :team-id) => true
       (provided
        (team-info account :orgname :team-id) => {:name owners-team-name}
        (team-members account :orgname :team-id) => [username]))

(fact "should update org settings"
      (let [params {:org org-name :description "test"}
            data {:form-params {:description "test"}
                  :content-type :json
                  :raw-response? true
                  :as-map? true}]
        (update account params) => org-profile
        (provided
          (make-url "orgs" org-name) => :url
          (parse-http :patch :url account data) => org-profile)))

(fact "should return all members team for an organization."
      (get-team account org-name internal-members-team-name)
      => {:teamid 1 :name internal-members-team-name}
      (provided
        (make-url (str "teams?org=" org-name)) => :url
        (parse-http :get :url account) => [{:teamid 1 :name internal-members-team-name}]))

(fact "should change default_role permissions on a project for a team"
      (share-team account :team-id :data) => :updated-team
      (provided
        (make-url "teams" :team-id "share") => :url
        (parse-http :post :url account {:form-params :data}) => :updated-team))
