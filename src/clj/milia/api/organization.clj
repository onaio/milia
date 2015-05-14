(ns milia.api.organization
  (:require [milia.api.http :refer [parse-http]]
            [milia.api.io :refer [make-url]]))

(def internal-members-team-name "members")

(def owners-team-name "Owners")

(def editor-role "editor")

(defn all [account]
  (let [url (make-url "orgs")]
    (parse-http :get url account)))

(defn create [account data]
  (let [url (make-url "orgs")]
    (parse-http :post url account
                {:form-params data})))

(defn profile [account org-name]
  (let [url (make-url "orgs" org-name)]
    (parse-http :get url account)))

(defn teams-all
  "Return all the teams for an organization."
  [account]
  (let [url (make-url "teams")]
    (parse-http :get url account)))

(defn teams
  "Return the teams for an organization, removing 'members' team that is used
   internall by the API to store non-team based org members."
  [account org-name]
  (let [teams (teams-all account)]
    (remove #(or (= internal-members-team-name (:name %))
                 (not= org-name (:organization %))) teams)))

(defn team-info [account org-name team-id]
  (let [url (make-url "teams" org-name team-id)]
    (parse-http :get url account)))

(defn team-members [account team-id]
  (let [url (make-url "teams" team-id "members")]
    (parse-http :get url account)))

(defn create-team
  "Add a team to an organization"
  [account params]
  (let [url (make-url "teams")]
    (parse-http :post url account {:form-params params})))

(defn add-team-member
  "Add a user to a team"
  [account org-name team-id user]
  (let [url (make-url "teams" org-name team-id "members")]
    (parse-http :post url account {:form-params user})))

(defn members [account org-name]
  (let [url (make-url "orgs" org-name "members")]
    (parse-http :get url account)))

(defn add-member
  "Add a user to an organization"
  ([account org-name member]
    (add-member account org-name member nil))
  ([account org-name member role]
    (let [url (make-url "orgs" org-name "members")
          assigned-role (if role
                          role
                          editor-role)]
      (parse-http :post url account {:form-params {:username member :role assigned-role}}))))

(defn remove-member
  "Remove a user from an organization or organization team"
  ([account org-name member]
     (remove-member account org-name member nil))
  ([account org-name member team-id]
     (let [url (if team-id
                 (make-url "teams" org-name team-id "members")
                 (make-url "orgs" org-name "members"))]
       (parse-http :delete url account {:query-params {:username member}}))))

(defn single-owner?
  "Is the user the only member of the Owners team."
  ([team members]
     (and (= owners-team-name (:name team))
          (= 1 (count members))))
  ([account org-name team-id]
     (single-owner?
      (team-info account org-name team-id)
      (team-members account org-name team-id))))

(defn single-owner-member?
  "Is user only members in org with owner role?"
  [account org-name]
  (let [org (profile account org-name)
        users (:users org)
        owner-roles (filter #(= "owner" %) (map :role users))]
    (= (count owner-roles) 1)))

(defn update
  "update organization profile"
  [account params]
  (let [url (make-url "orgs" (:org params))
        params (dissoc params :org)]
    (parse-http :patch url account {:form-params params
                                    :content-type :json
                                    :raw-response? true
                                    :as-map? true})))

(defn get-team
  "Returns an Organizaion team given the team name."
  [account org-name team-name]
  (let [url (make-url (str "teams?org=" org-name))
        teams (parse-http :get url account)]
    (first (remove #(not= team-name (:name %)) teams))))

(defn share-team [account team-id data]
  "Changes default_role permissions on a project for a team"
  (let [url (make-url "teams" team-id "share")]
    (parse-http :post url account {:form-params data})))
