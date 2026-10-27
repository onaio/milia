(ns milia.api.v2.organization
  "Organizations through the v2 API.

   Unlike v1, an organization does not carry its `users`: its members, with
   their roles, come from `members`, and the role of the user making the
   request is the organization's `current_user_role`. The list only carries
   what identifies each organization."
  (:refer-clojure :exclude [update])
  (:require [milia.api.http :refer [parse-http]]
            [milia.utils.remote :refer [make-v2-url]]))

(def editor-role "editor")

(defn all
  "List the organizations the account making the request belongs to.
   `shared-with` keeps only those the user with that username also belongs
   to, and `role` only those where the account making the request holds that
   role or a higher one."
  [& {:keys [shared-with role]}]
  (let [params (cond-> {}
                 shared-with (assoc :shared_with shared-with)
                 role (assoc :role role))]
    (parse-http :get
                (make-v2-url "orgs.json")
                :http-options {:query-params params})))

(defn create [data]
  (let [url (make-v2-url "orgs.json")]
    (parse-http :post url :http-options {:form-params data}
                :suppress-4xx-exceptions? true
                :as-map? true)))

(defn profile
  [org-name & {:keys [no-cache?]}]
  (when (seq org-name)
    (let [url (make-v2-url "orgs" (str org-name ".json"))]
      (parse-http :get url :no-cache? no-cache?))))

(defn retrieve-org-metadata
  [username]
  (:metadata (profile username)))

(defn update
  "update organization profile"
  [params]
  (let [org-name (:org params)
        url (make-v2-url "orgs" (str org-name ".json"))
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

(defn members
  "Every member of an organization with their role, as v1 returned them in
   the organization's `users`."
  [org-name]
  (parse-http :get (make-v2-url "orgs" org-name "members.json")))

(defn add-member
  "Add a user to an organization"
  ([org-name member]
   (add-member org-name member nil))
  ([org-name member role]
   (let [url (make-v2-url "orgs" org-name "members.json")
         data {:username member
               :role (or role editor-role)}]
     (parse-http :post
                 url
                 :http-options
                 #?(:clj   {:form-params data
                            :content-type :json})
                 #?(:cljs  {:json-params data})
                 :suppress-4xx-exceptions? true
                 :as-map? true))))

(defn change-member-role
  "Change the role of an organization member"
  [org-name member role]
  (let [data {:username member
              :role role}]
    (parse-http :put
                (make-v2-url "orgs" org-name "members.json")
                :http-options
                #?(:clj   {:form-params data
                           :content-type :json})
                #?(:cljs  {:json-params data})
                :as-map? true)))

(defn remove-member
  "Remove a user from an organization. v2 has no DELETE on the members: a
   member is removed by updating them with `remove`."
  [org-name member]
  (let [data {:username member
              :remove true}]
    (parse-http :patch
                (make-v2-url "orgs" org-name "members.json")
                :http-options
                #?(:clj   {:form-params data
                           :content-type :json})
                #?(:cljs  {:json-params data})
                :as-map? true)))

;; CLJ only: in CLJS `members` returns a channel, not the members
#?(:clj
   (defn single-owner-member?
     "Is user only members in org with owner role?"
     [org-name]
     (->> (members org-name)
          (filter #(= "owner" (:role %)))
          count
          (= 1))))
