(ns milia.api.user-test
  (:refer-clojure :exclude [get update])
  (:require [clojure.string :refer [join]]
            [midje.sweet :refer :all]
            [milia.api.user :refer :all]
            [milia.api.http :refer [parse-http]]
            [milia.utils.remote :refer [make-url]]))

(def username  "fake-username")
(def username2 "fake-username2")
(def username3 "fake-username3")
(def password  "fake-password")
(def account   {:username username :password password})
(def verification-key :fake-verification-key)
(def verification-key-uri (str "verify_email.json?verification_key="
                               verification-key))
(def username-uri  (str username ".json"))
(def redirect-url :fake-redirect-url)

(let [url :fake-url
      default-params {:city         ""
                      :country      ""
                      :email        ""
                      :gravatar     ""
                      :name         ""
                      :is_org       ""
                      :owner        ""
                      :require_auth ""
                      :twitter      ""
                      :url          ""
                      :user         ""
                      :username     ""
                      :website      ""
                      :organization ""}
      params {:first_name "fake-first-name"
              :last_name  "fake-last-name"
              :username   "fake-username"
              :email      "fake-email"
              :password "fake-password"}
      update-params (merge default-params
                           {:first_name "fake-first-name"
                            :last_name  "fake-last-name"
                            :email      "fake-email"
                            :city       "fake-city"
                            :country    "fakecountry"
                            :org        "fake-org"
                            :website    "fake-website"})
      data {:form-params params}
      updated-data {:form-params update-params
                    :content-type :json}
      url-path-for-multiple-profiles
      (str "profiles.json?users="
           (join "," [username username2 username3]))]

  (facts "About email verification"
         (fact "Should throw error if verification-key is missing"
               (verify-email nil) => (throws AssertionError))

         (fact "Should get correct url"
               (verify-email verification-key) => :something
               (provided
                (make-url "profiles"
                          verification-key-uri) => url
                (parse-http :get url
                            :suppress-4xx-exceptions? true) => :something))

         (fact "Should return an error"
               (verify-email verification-key) => nil
               (provided
                (make-url "profiles"
                          verification-key-uri) => url
                (parse-http :get url
                            :suppress-4xx-exceptions? true) =>
                {:detail :error})))

  (facts "About sending verification email"
         (fact "Should throw error if username is missing"
               (send-verification-email nil) => (throws AssertionError))

         (fact "Should post email verification data"
               (send-verification-email username) => :something
               (provided
                (make-url "profiles" "send_verification_email.json") => url
                (parse-http
                 :post url
                 :http-options
                 {:form-params {:username username}}) => :something)))

  (facts "About user-profile"
         (fact "Should throw if no username"
               (profile nil) => (throws AssertionError))

         (fact "Should get correct url"
               (profile username) => :something
               (provided
                (make-url "profiles" username-uri) => url
                (parse-http :get url
                            :suppress-4xx-exceptions? true) => :something))

         (fact "Should get correct url"
               (profile username) => nil
               (provided
                (make-url "profiles" username-uri) => url
                (parse-http :get url
                            :suppress-4xx-exceptions? true) =>
                {:detail :error})))

  (facts "About getting profiles for a list of users"
         (fact "Should get correct url"
               (get-profiles-for-list-of-users [username
                                                username2
                                                username3]) => :something
               (provided
                (make-url url-path-for-multiple-profiles) => url
                (parse-http :get url
                            :suppress-4xx-exceptions? true) => :something))
         (fact "Should not allow an empty list of users"
               (get-profiles-for-list-of-users [])
               => (throws AssertionError)))

  (facts "About user"
         (fact "Should get correct url"
               (user false) => :something
               (provided
                (make-url "user.json") => url
                (parse-http :post url
                            :suppress-4xx-exceptions? false) => :something)))

  (facts "About create"
         (fact "Should register a new user"
               (create params) => :someone
               (provided
                (make-url "profiles.json") => url
                (parse-http :post url :http-options data) => :someone)))

  (facts "About all"
         (fact "Should get users")
         (all) => :userlist
         (provided
          (make-url "users.json") => url
          (parse-http :get url) => :userlist))

  (fact "About generating odk token"
        (generate-odk-token) => :response
        (provided
         (make-url "user" "odk_token.json") => :url
         (parse-http :post :url) => :response))

  (facts "About update"
         (fact "Should put to profiles"
               (update username update-params) => :updated-profile
               (provided
                (make-url "profiles" username-uri) => url
                (parse-http :put url
                            :http-options updated-data
                            :as-map? true)
                => :updated-profile)))

  (facts "About user change-password"
         (fact "Should post to profiles with passwords"
               (change-password username
                                :current_password :new_password) => :updated
               (provided
                (make-url "profiles" username "change_password.json") => url
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
                (make-url "profiles" username-uri) => url
                (retrieve-metadata username) => {:random "test"}
                (parse-http :patch
                            url
                            :http-options
                            {:form-params {:metadata {:first-login false
                                                      :random "test"}}
                             :content-type :json}
                            :as-map? true
                            :suppress-4xx-exceptions? nil) => :metadata)))

  (facts "About get-email"
         (fact "Should get users for username"
               (get username) => :user
               (provided
                (make-url "users" username-uri) => url
                (parse-http :get url)
                => :user)))

  (facts "About get-by-email"
         (fact "Should get users for email address"
               (get-by-email :email) => :user
               (provided
                (make-url "users.json") => url
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
         (make-url "user" "reset.json") => api-endpoint-url
         (parse-http
          :post api-endpoint-url :http-options {:form-params params}) => nil)))

(fact "reset-password should call the reset password endpoint"
      (let [new-password "spiffing new password"
            token "aT0k3n"
            uid "MtJs"
            params {:new_password new-password :token token :uid uid}]
        (reset-password new-password
                        token
                        uid
                        :suppress-4xx-exceptions? true) => nil
        (provided
         (make-url "user" "reset.json") => :url
         (parse-http :post :url
                     :suppress-4xx-exceptions? true
                     :http-options {:form-params params}) => nil)))

(fact "patch should submit a patch request"
      (patch "username" :params) => :response
      (provided
       (make-url "profiles" "username.json") => "url"
       (parse-http :patch
                   "url"
                   :http-options {:form-params :params
                                  :content-type :json}
                   :as-map? true
                   :suppress-4xx-exceptions? nil) => :response))

(fact "change-email-address should submit a partial update for email"
      (change-email-address username :email :password) => :response
      (provided
       (patch username {:email :email :password :password}
              :suppress-4xx-exceptions? true) => :response))

(fact "expire-temp-token should call delete on the expire endpoint"
      (expire-temp-token) => :response
      (provided
       (make-url "user" "expire.json") => :url
       (parse-http :delete :url) => :response))

(fact "expire-temp-token should call delete on the expire endpoint"
      (google-sheet-authorization :code :redirect_uri) => :response
      (provided
       (make-url "google"
                 (format "google_auth.json?code=%s&redirect_uri=%s"
                         :code :redirect_uri)) => :url
       (parse-http :get :url :as-map? true :suppress-4xx-exceptions? true)
       => :response))

(facts "about subscription"
       (let [username "fake-user"
             contactpersons [{:contactperson_id "622783000000599003"
                              :email "goodgreat@ona.io"}
                             {:contactperson_id "622783000000689005"
                              :email "goodbetter@gmail.com"}]]
         (fact "should get subscription"
               (get-subscription username) => :body
               (provided
                (make-url "pricing" "subscriptions" username) => :url
                (parse-http :get :url
                            :suppress-4xx-exceptions? true) => :body))
         (fact "should update subscription"
               (update-subscription username contactpersons) => :contactpersons
               (provided
                (make-url "pricing" "subscriptions" username) => :url
                (parse-http
                 :patch :url
                 :http-options {:form-params {:contactpersons contactpersons}}
                 :suppress-4xx-exceptions? true) => :contactpersons))))
