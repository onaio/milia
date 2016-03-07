(ns milia.api.project-test
  (:refer-clojure :exclude [update])
  (:require [midje.sweet :refer :all]
            [milia.api.project :refer :all]
            [milia.api.http :refer [parse-http]]
            [milia.utils.remote :refer [make-url]]
            [milia.utils.file :as f]))

(let [url :fake-url
      callback :fake-callback
      username :fake-username
      logged-in-username "johndoe"
      password :fake-password
      data {:url "a/b/c/id"}
      data-with-owner (merge data
                             {:owner url})
      parsed-data (merge data {:id "id"})]

  (facts "about projects all"
         (fact "Should get correct url"
               (all) => :response
               (provided
                (make-url "projects") => url
                (parse-http :get url :http-options {:query-params nil}
                            :no-cache? nil) => :response))

         (fact "Should pass owner as a query parameter"
               (all username) => :response
               (provided
                (make-url "projects") => url
                (parse-http :get
                            url
                            :http-options {:query-params
                                           {:owner username}}
                            :no-cache? nil) => :response))

         (fact "Should pass logged-in-user as a query parameter"
               (all username
                    :logged-in-username logged-in-username) => :response
               (provided
                (make-url "projects") => url
                (parse-http :get
                            url
                            :http-options {:query-params
                                           {:owner username
                                            :u logged-in-username}}
                            :no-cache? nil) => :response)))

  (facts "about project-create"
         (fact "Should associate data"
               (create data username) => parsed-data
               (provided
                (make-url "users" username) => url
                (make-url "projects") => url
                (parse-http :post
                            url
                            :http-options {:form-params data-with-owner
                                           :content-type :json}) => data))

         (fact "Should throw an exception if special __all__ error key returned"
               (let [error :error]
                 (create data username) => (throws clojure.lang.ExceptionInfo)
                 (provided
                  (make-url "users" username) => url
                  (make-url "projects") => url
                  (parse-http :post
                              url
                              :http-options {:form-params data-with-owner
                                             :content-type :json})
                  => {:__all__ error}))))

  (facts "about get-project"
         (fact "Should find project for id"
               (get-project :id) => parsed-data
               (provided
                (make-url "projects" :id) => url
                (parse-http :get url :no-cache? nil) => data))

         (fact "Should handle when parse-http returns a string"
               (get-project :id) => nil
               (provided
                (make-url "projects" :id) => url
                (parse-http :get url :no-cache? nil) => "a string")))

  (facts "about get-forms"
         (fact "Should find forms for id"
               (get-forms :id) => data
               (provided
                (make-url "projects" :id "forms") => url
                (parse-http :get url) => data)))

  (facts "about share for project"
         (fact "Should return result of parse-http"
               (let [role :fake-role
                     data {:username username :role role}
                     data-remove (merge data {:remove "True"})]
                 (share :id username role) => :204
                 (provided
                  (make-url "projects" :id "share") => url
                  (parse-http :put url
                              :http-options {:form-params data}) => :204)

                 (share :id username role true) => :204
                 (provided
                  (make-url "projects" :id "share") => url
                  (parse-http :put url
                              :http-options {:form-params
                                             data-remove}) => :204))))

  (facts "about update for project"
         (fact "Should return result of parse-http"
               (let [owner :fake-owner
                     name :fake-project-name
                     metadata :fake-metadata
                     data {:owner owner :name name :metadata metadata}]
                 (update :id data) => :updated-project
                 (provided
                  (make-url "projects" :id) => url
                  (parse-http :patch
                              url
                              :http-options {:form-params data
                                             :content-type :json})
                  => :updated-project))))

  (facts "about add-tags"
         (fact "Should call parse-http with tags"
               (add-tags :id [:tag1 :tag2]) => :response
               (provided
                (make-url "projects" :id "labels") => url
                (parse-http :post url
                            :http-options {:form-params {:tags ":tag1,:tag2"}
                                           :content-type :json})
                => :response)))

  (facts "about starring"
         (fact "add-star should post star"
               (add-star :id) => :response
               (provided
                (make-url "projects" :id "star") => url
                (parse-http :post url :callback nil) => :response))

         (fact "add-star with callback should also post star"
               (add-star :id :callback callback) => :response
               (provided
                (make-url "projects" :id "star") => url
                (parse-http :post url :callback callback) => :response))

         (fact "remove-star should delete to star"
               (remove-star :id) => :response
               (provided
                (make-url "projects" :id "star") => url
                (parse-http :delete url :callback nil) => :response))

         (fact "remove-star with callback should also delete star"
               (remove-star :id :callback callback) => :response
               (provided
                (make-url "projects" :id "star") => url
                (parse-http :delete url :callback callback) => :response))

         (fact "get-starred should get to star no id"
               (get-starred username) => :response
               (provided
                (make-url "user" username "starred") => url
                (parse-http :get url) => :response))

         (fact "starred-by should get to star with id"
               (starred-by :id) => :response
               (provided
                (make-url "projects" :id "star") => url
                (parse-http :get url) => :response)))

  (facts "about delete"
         (fact "should call parse-http with delete"
               (delete :id) => :response
               (provided
                (make-url "projects" :id) => url
                (parse-http :delete url) => :response)))

  (facts "about project owner transfer"
         (fact "should call parse-http with patch"
               (transfer-owner :id :new_owner) => :response
               (provided
                (make-url "projects" :id) => url
                (make-url "users" :new_owner) => :user-url
                (parse-http :patch
                            url
                            :http-options {:form-params {:owner :user-url}
                                           :content-type :json})
                => :response)))
  (fact "about upload project files"
        (upload-file :project-id {:filename "image.png"}) => :response
        (provided
         (f/uploaded->file {:filename "image.png"}) => :file
         (make-url "metadata") => url
         (parse-http :post
                     url
                     :http-options {:multipart [{:name "data_value"
                                                 :content "image.png"}
                                                {:name "data_type"
                                                 :content "supporting_doc"}
                                                {:name "project"
                                                 :content :project-id}
                                                {:name "data_file"
                                                 :content :file}]}
                     :suppress-4xx-exceptions? true)
         => :response)))
