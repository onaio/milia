(ns milia.api.project_test
  (:require [midje.sweet :refer :all]
            [milia.api.project :refer :all]
            [milia.api.http :refer [parse-http]]
            [milia.api.io :refer [make-url]]))

(let [url :fake-url
      username :fake-username
      password :fake-password
      account {:username username :password password}
      data {:url "a/b/c/id"}
      data-with-owner (merge data
                             {:owner url})
      parsed-data (merge data {:id "id"})]

  (facts "about projects all"
         (fact "Should get correct url"
               (all account) => :response
               (provided
                (make-url "projects") => url
                (parse-http :get url account nil) => :response))

         (fact "Should pass owner as a query parameter"
               (all account username) => :response
               (provided
                (make-url "projects") => url
                (parse-http :get
                            url
                            account {:query-params
                                     {:owner username}}) => :response)))

  (facts "about project-create"
         (fact "Should associate data"
               (create account data username) => parsed-data
               (provided
                (make-url "users" username) => url
                (make-url "projects") => url
                (parse-http :post
                            url
                            account
                            {:form-params data-with-owner
                             :content-type :json}) => data))

         (fact "Should throw an exception if special __all__ error key returned"
               (let [error :error]
                 (create account data username) => (throws clojure.lang.ExceptionInfo)
                 (provided
                  (make-url "users" username) => url
                  (make-url "projects") => url
                  (parse-http :post
                              url
                              account
                              {:form-params data-with-owner
                               :content-type :json}) => {:__all__ error}))))

  (facts "about get-project"
         (fact "Should find project for id"
               (get-project account :id) => parsed-data
               (provided
                (make-url "projects" :id) => url
                (parse-http :get
                            url
                            account) => data)))

  (facts "about get-forms"
         (fact "Should find forms for id"
               (get-forms account :id) => data
               (provided
                (make-url "projects" :id "forms") => url
                (parse-http :get
                            url
                            account) => data)))

  (facts "about share for project"
         (fact "Should return result of parse-http"
               (let [role :fake-role
                     data {:username username :role role}
                     data-remove (merge data {:remove "True"})]
                 (share account :id username role) => :204
                 (provided
                  (make-url "projects" :id "share") => url
                  (parse-http :put url account {:form-params data}) => :204)

                 (share account :id username role true) => :204
                 (provided
                  (make-url "projects" :id "share") => url
                  (parse-http :put url account {:form-params data-remove}) => :204))))

  (facts "about update for project"
         (fact "Should return result of parse-http"
               (let [owner :fake-owner
                     name :fake-project-name
                     metadata :fake-metadata
                     data {:owner owner :name name :metadata metadata}]
                 (update account :id data) => :updated-project
                 (provided
                  (make-url "projects" :id) => url
                  (parse-http :patch
                              url
                              account
                              {:form-params data
                               :content-type :json}) => :updated-project))))

  (facts "about add-tags"
         (fact "Should call parse-http with tags"
               (add-tags account :id [:tag1 :tag2]) => :response
               (provided
                (make-url "projects" :id "labels") => url
                (parse-http :post url account {:form-params {:tags ":tag1,:tag2"}
                                               :content-type :json}) => :response)))

  (facts "about starring"
         (fact "add-star should post to star"
               (add-star account :id) => :response
               (provided
                (make-url "projects" :id "star") => url
                (parse-http :post url account) => :response))

         (fact "remove-star should delete to star"
               (remove-star account :id) => :response
               (provided
                (make-url "projects" :id "star") => url
                (parse-http :delete url account) => :response))

         (fact "get-starred should get to star no id"
               (get-starred account) => :response
               (provided
                (make-url "user" username "starred") => url
                (parse-http :get url account) => :response))

         (fact "starred-by should get to star with id"
               (starred-by account :id) => :response
               (provided
                (make-url "projects" :id "star") => url
                (parse-http :get url account) => :response)))

  (facts "about delete"
         (fact "should call parse-http with delete"
               (delete account :id) => :response
               (provided
                (make-url "projects" :id) => url
                (parse-http :delete url account) => :response)))

  (facts "about project owner transfer"
         (fact "should call parse-http with patch"
               (transfer-owner account :id :new_owner) => :response
               (provided
                (make-url "projects" :id) => url
                (make-url "users" :new_owner) => :user-url
                (parse-http :patch
                            url
                            account
                            {:form-params {:owner :user-url}
                             :content-type :json}) => :response))))
