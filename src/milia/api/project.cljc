(ns milia.api.project
  (:refer-clojure :exclude [update])
  (:require [clojure.string :refer [join]]
            [milia.api.http :refer [parse-http]]
            [milia.utils.remote :refer [make-url]]
            [milia.utils.url :refer [last-url-param]]
            #?(:clj [slingshot.slingshot :refer [throw+]])))

(defn- add-id
  "Parse and add the projects ID."
  [project-data]
  (when-not (string? project-data)
    (merge project-data
           {:id (-> project-data :url last-url-param)})))

(defn get-forms
  "Get the forms for this account and owner of the user."
  [id]
  (let [url (make-url "projects" id "forms")]
    (parse-http :get url)))

(defn get-project [id & {:keys [no-cache?]}]
  (let [url (make-url "projects" id)
        data (parse-http :get url :no-cache? no-cache?)]
    #?(:clj (add-id data) :cljs data)))

(defn all
  "Return all project for this account and owner or the user."
  ([]
   (all nil))
  ([owner & {:keys [no-cache? logged-in-username]}]
   (let [url (make-url "projects")
         options (->
                  {:query-params nil}
                  (#(if owner (assoc-in % [:query-params :owner] owner) %))
                  (#(if logged-in-username (assoc-in % [:query-params :u]
                                                      logged-in-username) %)))]
     (parse-http :get url
                 :http-options options
                 :no-cache? no-cache?))))

(defn create
  "Create a project for this account and owner or the user."
  [data owner]
   (let [owner-url {:owner (make-url "users" owner)}
         url (make-url "projects")
         form-params (merge owner-url data)
         #?(:clj project-data)
         #?(:clj (parse-http :post
                             url
                             :http-options {:form-params form-params
                                            :content-type :json}))]
     #?(:clj
        (if-let [error (:__all__ project-data)]
          (throw+ error)
          (add-id project-data)))
     #?(:cljs
        (parse-http :post url :http-options {:json-params form-params}))))

(defn update
  "Update project metadata"
  [project-id data]
  (let [url (make-url "projects" project-id)]
    (parse-http :patch url :http-options {:form-params data
                                          :content-type :json})))

(defn share
  "Share project with specific user or remove specific user from project"
  ([project-id username role]
    (share project-id username role false))
  ([project-id username role remove?]
    (let [url (make-url "projects" project-id "share")
          data {:username username :role role}
          form-params (if remove?
                        (merge data {:remove "True"})
                        data)]
      (parse-http :put url :http-options {:form-params form-params}))))

(defn add-tags
  "Add tags to a project."
  [id tags]
  (let [url (make-url "projects" id "labels")]
    (parse-http :post url :http-options {:form-params {:tags (join "," tags)}
                                         :content-type :json})))

(defn with-tag
  "Get projects with given tags."
  [tags]
  (let [url (make-url "projects")]
    (parse-http :get url :http-options {:query-params
                                        {:tags (join "," tags)}})))

(defn add-star
  "Add star to project for this user."
  [id & {:keys [callback]}]
  (let [url (make-url "projects" id "star")]
    (parse-http :post url :callback callback)))

(defn remove-star
  "Remove star from project for this user."
  [id & {:keys [callback]}]
  (let [url (make-url "projects" id "star")]
    (parse-http :delete url :callback callback)))

(defn toggle-star
  "Toggle between starred and unstarred for a user's project"
  [id star? callback]
    ((if star? add-star remove-star) id :callback callback))

(defn get-starred
  "Get projects this user has starred."
  ([username]
     (let [url (make-url "user" username "starred")]
       (parse-http :get url))))

(defn starred-by
  "Get user that starred this project."
  [id]
  (let [url (make-url "projects" id "star")]
    (parse-http :get url)))

(defn delete
  "Delete a project"
  [id]
  (let [url (make-url "projects" id)]
    (parse-http :delete url)))

(defn transfer-owner
  "Set new project owner"
  [id new-owner]
  (let [url (make-url "projects" id)
        new-owner (make-url "users" new-owner)
        form-params {:owner new-owner}]
    (parse-http :patch url :http-options {:form-params form-params
                                          :content-type :json})))

(defn update-public
  "Update the project public setting."
  [projectid public]
  (update projectid {:public public}))
