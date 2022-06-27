(ns milia.api.organization
  (:refer-clojure :exclude [update])
  (:require [milia.api.http :refer [parse-http]]
            [milia.utils.remote :refer [make-url]]
            #?(:cljs [cljs.core.async :refer [put!]])))

(def internal-members-team-name "members")

(def owners-team-name "Owners")

(def editor-role "editor")

(defn all
  "List all the organizations belonging to the account making the request.
   When a username is provided, return only those organizations shared by both
   the account making the request and the user associated with the username."
  [& [username]]
  (let [url (make-url (if username
                        (str "orgs.json?shared_with=" username)
                        "orgs.json"))]
    (parse-http :get url)))

(defn create [data]
  (let [url (make-url "orgs.json")]
    (parse-http :post url :http-options {:form-params data}
                :suppress-4xx-exceptions? true
                :as-map? true)))
(defn profile
  [org-name & {:keys [no-cache?]}]
  (when (seq org-name)
    (let [url (make-url "orgs" (str org-name ".json"))]
      (parse-http :get url :no-cache? no-cache?))))

(defn retrieve-org-metadata
  [username]
  (:metadata (profile username)))

(defn can-user-create-project-under-organization?
  "Return whether a user can create projects within an organization"
  [username-to-check organization]
  (let [role
        (->> organization
             :users
             (filter #(= (:user %) username-to-check))
             first
             :role)]
    (or (= role "manager")
        (= role "owner"))))

(defn get-organizations-where-user-can-create-projects
  [username-to-check]
  (filter #(can-user-create-project-under-organization?
            username-to-check %) (all)))

(defn teams-all
  "Return all the teams for an organization."
  ([]
   (teams-all nil))
  ([organization-name]
   (let [base-url (make-url "teams.json")
         url (if organization-name
               (str base-url "?org=" organization-name)
               base-url)]
     (parse-http :get url))))

(defn teams
  "Return the teams for an organization, removing 'members' team that is used
   internall by the API to store non-team based org members."
  [org-name]
  (let [teams (teams-all)]
    (remove #(or (= internal-members-team-name (:name %))
                 (not= org-name (:organization %))) teams)))

(defn team-info [org-name team-id]
  (let [url (make-url "teams" org-name (str team-id ".json"))]
    (parse-http :get url)))

(defn team-members [team-id]
  (let [url (make-url "teams" team-id "members.json")]
    (parse-http :get url)))

(defn create-team
  "Add a team to an organization"
  [params]
  (let [url (make-url "teams.json")]
    (parse-http :post url :http-options {:form-params params})))

(defn add-team-member
  "Add a user to a team"
  [org-name team-id user]
  (let [url (make-url "teams" org-name team-id "members.json")]
    (parse-http :post url :http-options {:form-params user})))

(defn members [org-name]
  (let [url (make-url "orgs" org-name "members.json")]
    (parse-http :get url)))

(defn add-member
  "Add a user to an organization"
  ([org-name member]
   (add-member org-name member nil))
  ([org-name member role]
   (let [url (make-url "orgs" org-name "members.json")
         assigned-role (or role editor-role)]
     (parse-http :post
                 url
                 :http-options
                 #?(:clj   {:form-params {:username member
                                          :role assigned-role}
                            :content-type :json})
                 #?(:cljs  {:json-params {:username member
                                          :role assigned-role}})
                 :suppress-4xx-exceptions? true
                 :as-map? true))))

(defn change-org-member-role
  "Change the role of an organization member"
  [member org-name event-chan]
  (let [data {:username (:username member)
              :role (:role member)}]
    (parse-http :put
                (make-url "orgs" org-name "members.json")
                :callback
                #?(:clj nil
                   :cljs #(put! event-chan {:updated-member member}))
                :http-options
                #?(:clj   {:form-params data
                           :content-type :json})
                #?(:cljs {:json-params data})
                :as-map? true)))

(defn remove-member
  "Remove a user from an organization or organization team"
  ([org-name member]
   (remove-member org-name member nil))
  ([org-name member team-id]
   (let [url (if team-id
               (make-url "teams" org-name team-id "members.json")
               (make-url "orgs" org-name "members.json"))]
     (parse-http :delete url
                 :http-options {:query-params {:username member}}))))

(defn single-owner?
  "Is the user the only member of the Owners team."
  ([team members]
   (and (= owners-team-name (:name team))
        (= 1 (count members)))))

(defn single-owner-member?
  "Is user only members in org with owner role?"
  [org-name]
  (let [org (profile org-name)
        users (:users org)
        owner-roles (filter #(= "owner" %) (map :role users))]
    (= (count owner-roles) 1)))

(defn update
  "update organization profile"
  [params]
  (let [org-name (:org params)
        url (make-url "orgs" (str org-name ".json"))
        params (dissoc params :org)]
    (when (seq org-name)
      (parse-http
       :patch
       url
       :http-options
       #?(:clj   {:form-params params
                  :content-type :json})
       #?(:cljs  {:json-params params})
       :as-map? true))))

(defn get-team
  "Returns an Organizaion team given the team name."
  [org-name team-name]
  (let [url (make-url (str "teams.json?org=" org-name))
        teams (parse-http :get url :suppress-4xx-exceptions? true)]
    (first (remove #(not= team-name (:name %)) teams))))

(defn share-team
  "Changes default_role permissions on a project for a team"
  [team-id data]
  (let [url (make-url "teams" team-id "share.json")]
    (parse-http :post url
                :http-options
                #?(:clj   {:form-params data
                           :content-type :json})
                #?(:cljs  {:json-params data})
                :as-map? true)))
