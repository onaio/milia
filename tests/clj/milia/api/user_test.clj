(ns milia.api.user_test
  (:refer-clojure :exclude [update])
  (:require [midje.sweet :refer :all]
            [milia.api.user :refer :all]
            [milia.api.http :refer [parse-http]]
            [milia.utils.remote :refer [make-url]]))

(def username :fake-username)
(def password :fake-password)
(def account {:username username :password password})

(let [url :fake-url
      default-params {:city ""
                      :country ""
                      :email ""
                      :gravatar ""
                      :name ""
                      :is_org ""
                      :owner ""
                      :require_auth ""
                      :twitter ""
                      :url ""
                      :user ""
                      :username ""
                      :website ""
                      :organization ""}
      params {:first_name "fake-first-name"
              :last_name "fake-last-name"
              :username "fake-username"
              :email "fake-email"
              :password "fake-password"}
      update-params (merge default-params
                           {:first_name "fake-first-name"
                            :last_name "fake-last-name"
                            :email "fake-email"
                            :city "fake-city"
                            :country "fakecountry"
                            :org "fake-org"
                            :website "fake-website"})
      data {:form-params params}
      updated-data {:form-params update-params
                    :content-type :json}]

  (facts "About user-profile"
         (fact "Should get correct url"
               (profile username) => :something
               (provided
                (make-url "profiles" username) => url
                (parse-http :get url :suppress-4xx-exceptions? true) => :something)))

  (facts "About user"
         (fact "Should get correct url"
               (user false) => :something
               (provided
                (make-url "user") => url
                (parse-http :get url :suppress-4xx-exceptions? false) => :something)))

  (facts "About create"
         (fact "Should register a new user"
               (create params) => :someone
               (provided
                (make-url "profiles") => url
                (parse-http :post url :http-options data) => :someone)))

  (facts "About all"
         (fact "Should get users")
         (all) => :userlist
         (provided
          (make-url "users") => url
          (parse-http :get url) => :userlist))

  (facts "About update"
         (fact "Should put to profiles"
               (update username update-params) => :updated-profile
               (provided
                (make-url "profiles" username) => url
                (parse-http :put url
                            :http-options updated-data
                            :as-map? true)
                => :updated-profile)))

  (facts "About user change-password"
         (fact "Should post to profiles with passwords"
               (change-password username :current_password :new_password) => :updated
               (provided
                (make-url "profiles" username "change_password") => url
                (parse-http :post
                            url
                            :http-options
                            {:form-params {:current_password :current_password
                                           :new_password :new_password}}
                            :raw-response? true
                            :suppress-4xx-exceptions? true
                            :as-map? true) => :updated)))

  (facts "About metadata"
         (fact "Should return metadata"
               (retrieve-metadata :fake-username) => :metadata
               (provided
                (profile :fake-username) => {:metadata :metadata}))

         (fact "should update metadata"
               (update-user-metadata username {:first-login false}) => :metadata
               (provided
                 (make-url "profiles" username) => url
                 (retrieve-metadata username) => {:random "test"}
                 (parse-http :patch
                             url
                             :http-options
                             {:form-params {:metadata {:first-login false
                                                       :random "test"}}
                              :content-type :json}
                             :as-map? true
                             :suppress-4xx-exceptions? nil) => :metadata)))

  (facts "About get-by-email"
         (fact "Should get users for email address"
               (get-by-email :email) => :user
               (provided
                (make-url "users") => url
                (parse-http :get url
                            :http-options {:query-params {:search :email}}
                            :suppress-4xx-exceptions? true)
                => :user))))

(fact "trigger-password-reset-email should call the reset endpoint"
      (let [email "forgetful@example.com"
            subject "Reset my password"
            api-endpoint-url "/user/reset"
            reset-url "http://example.com"
            params {:email_subject subject :reset_url reset-url :email email}]
        (trigger-password-reset-email email reset-url subject) => nil
        (provided
         (make-url "user" "reset") => api-endpoint-url
         (parse-http
          :post api-endpoint-url :http-options {:form-params params}) => nil)))

(fact "reset-password should call the reset password endpoint"
      (let [new-password "spiffing new password"
            token "aT0k3n"
            uid "MtJs"
            params {:new_password new-password :token token :uid uid}]
        (reset-password new-password token uid) => nil
        (provided
         (make-url "user" "reset") => :url
         (parse-http :post :url :http-options {:form-params params}) => nil)))

(fact "patch should submit a patch request"
      (patch username :params) => :response
      (provided
       (make-url "profiles" username) => :url
       (parse-http :patch
                   :url
                   :http-options {:form-params :params
                                  :content-type :json}
                   :as-map? true
                   :suppress-4xx-exceptions? nil) => :response))

(fact "change-email-address should submit a partial update for email"
      (change-email-address account :email) => :response
      (provided
       (patch account {:email :email}
              :suppress-4xx-exceptions? true) => :response))

(fact "expire-temp-token should call delete on the expire endpoint"
      (expire-temp-token) => :response
      (provided
       (make-url "user" "expire") => :url
       (parse-http :delete :url) => :response))
