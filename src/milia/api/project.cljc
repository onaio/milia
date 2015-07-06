(ns milia.api.project
  (:require [clojure.string :refer [join]]
            [milia.api.http :refer [parse-http]]
            [milia.api.io :refer [make-url
                                  #?(:cljs query-helper!)]]
            [milia.utils.url :refer [last-url-param]]
            #?(:clj [slingshot.slingshot :refer [throw+]])))

(defn- add-id
  "Parse and add the projects ID."
  [project-data]
  (merge project-data
         {:id (-> project-data :url last-url-param)}))

(defn get-forms
  "Get the forms for this account and owner of the user."
  [account id]
  (let [url (make-url "projects" id "forms")]
    (parse-http :get url account)))

(defn get-project [account id]
  (let [url (make-url "projects" id)]
    (add-id (parse-http :get url account))))

(defn all
  "Return all project for this account and owner or the user."
  ([account]
     (all account nil))
  ([account owner]
     (let [url (make-url "projects")
           options (if-not (nil? owner) {:query-params {:owner owner}})]
       (parse-http :get url account options))))

(defn create
  "Create a project for this account and owner or the user."
  ([account data]
   (create account data (:username account)))
  ([account data owner]
   (let [owner-url {:owner (make-url "users" owner)}
         url (make-url "projects")
         form-params (merge owner-url data)
         #?(:clj project-data)
         #?(:clj (parse-http :post url account
                             {:form-params form-params
                             :content-type :json}))]
     #?(:clj
        (if-let [error (:__all__ project-data)]
          (throw+ error)
          (add-id project-data)))
     #?(:cljs
        (parse-http :post url account {:form-params form-params})))))

(defn update
  "Update project metadata"
  [account project-id data]
  (let [url (make-url "projects" project-id)]
    (parse-http :patch url account {:form-params data
                                    :content-type :json})))

(defn share
  "Share project with specific user or remove specific user from project"
  ([account project-id username role]
    (share account project-id username role false))
  ([account project-id username role remove?]
    (let [url (make-url "projects" project-id "share")
          data {:username username :role role}
          form-params (if remove?
                        (merge data {:remove "True"})
                        data)]
      (parse-http :put url account {:form-params form-params}))))

(defn add-tags
  "Add tags to a project."
  [account id tags]
  (let [url (make-url "projects" id "labels")]
    (parse-http :post url account {:form-params {:tags (join "," tags)}
                                   :content-type :json})))

(defn with-tag
  "Get projects with given tags."
  [account tags]
  (let [url (make-url "projects")]
    (parse-http :get url account {:query-params {:tags (join "," tags)}})))

(defn add-star
  "Add star to project for this user."
  [account id]
  (let [url (make-url "projects" id "star")]
    (parse-http :post url account)))

(defn remove-star
  "Remove star from project for this user."
  [account id]
  (let [url (make-url "projects" id "star")]
    (parse-http :delete url account)))

(defn get-starred
  "Get projects this user has starred."
  ([account]
     (get-starred account (:username account)))
  ([account username]
     (let [url (make-url "user" username "starred")]
       (parse-http :get url account))))

(defn starred-by
  "Get user that starred this project."
  [account id]
  (let [url (make-url "projects" id "star")]
    (parse-http :get url account)))

(defn delete
  "Delete a project"
  [account id]
  (let [url (make-url "projects" id)]
    (parse-http :delete url account)))

(defn transfer-owner
  "Set new project owner"
  [account id new-owner]
  (let [url (make-url "projects" id)
        new-owner (make-url "users" new-owner)
        form-params {:owner new-owner}]
    (parse-http :patch url account {:form-params form-params
                                    :content-type :json})))

#?(:cljs
   (defn update-project
     "Update the project"
     [projectid owner params]
     (let [url (str "/" owner "/" projectid "/project-settings")
           query-params (merge {:project-id projectid
                               :patch true}
                               params)]
       (query-helper! :post url nil query-params))))

#?(:cljs
   (defn update-public
     "Update the project public setting."
     [projectid owner public]
     (update-project projectid owner {:public public})))
