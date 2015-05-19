(ns milia.api.user
  (:require [milia.api.http :refer [parse-http]]
            [milia.api.io :refer [make-url]]
            [milia.utils.seq :refer [has-keys?]]
            [slingshot.slingshot :refer [throw+]]))

(defn patch
  [account params]
  (let [username (:username account)
        url (make-url "profiles" username)
        data {:form-params params
              :content-type :json
              :as-map? true}]
    (parse-http :patch url account data)))

(defn profile
  "Return the profile for the account username or the passed username."
  ([account]
     (profile account (:username account)))
  ([account username]
     (let [url (make-url "profiles" username)
           response (parse-http :get url account {:suppress-40x-exceptions? true})]
       (if-let [error (:detail response)]
         nil
         response))))

(defn user
  "Return the user profile with authentication details."
  ([account]
   (user account false))
  ([account use-temp-token?]
   (user account use-temp-token? false))
  ([account use-temp-token? suppress-40x-exception?]
   (let [url (make-url "user")
         response (parse-http
                   :get
                   url
                   account
                   {:use-temp-token use-temp-token?
                    :suppress-40x-exceptions? suppress-40x-exception?})]
     (if-let [error (:detail response)]
       (when use-temp-token? response)
       response))))

(defn create
  "Create a new user."
  [params]
  (let [profile (select-keys params [:first_name
                                     :last_name
                                     :username
                                     :email
                                     :password])
        url (make-url "profiles")
        data {:form-params profile}]
    (parse-http :post url nil data)))

(defn all
  "return all users"
  [account]
  (let [url (make-url "users")]
    (parse-http :get url account)))

(defn update
  "update user profile"
  [account params]
  {:pre [(has-keys? params [:city
                            :country
                            :email
                            :gravatar
                            :last_name
                            :first_name
                            :is_org
                            :owner
                            :require_auth
                            :twitter
                            :url
                            :user
                            :username
                            :website
                            :organization])]}
  (let [url (make-url "profiles" (:username account))]
    (parse-http :put url account {:form-params params
                                  :content-type :json
                                  :as-map? true})))

(defn change-password
  "Change user password"
  [account current-password new-password]
  (let [username (:username account)
        url (make-url "profiles" username "change_password")
        data {:form-params {:current_password current-password
                            :new_password new-password}
              :raw-response? true}]
    (parse-http :post url account data)))

(defn retrieve-metadata
  ([account]
   (retrieve-metadata account (:username account)))
  ([account username]
   (let [profile-info (profile account username)]
     (:metadata profile-info))))

(defn update-user-metadata
  [account metadata]
  (let [current-metadata (retrieve-metadata account)
        updated-metadata (merge current-metadata metadata)]
    (patch account {:metadata updated-metadata})))

(defn get-by-email
  "Return the users that match this email address"
  [account email]
  (let [url (make-url "users")]
    (parse-http :get url account {:query-params {:search email}
                                  :suppress-40x-exceptions? true})))

(defn trigger-password-reset-email
  [email reset-url email-subject]
  (let [url (make-url "user" "reset")]
    (parse-http
     :post
     url
     nil ;; Unauthenticated API request does not need an account
     {:form-params
      {:email email
       :reset_url reset-url
       :email_subject email-subject}})))

(defn reset-password
  [new-password token uid]
  (let [url (make-url "user" "reset")]
    (parse-http
     :post
     url
     nil
     {:form-params
      {:new_password new-password
       :token token
       :uid uid}})))

(defn change-email-address
  "Change the user's email address"
  [account email-address]
  (let [params {:email email-address}]
    (patch account params)))

(defn expire-temp-token
  "Expire the user's temporary token."
  [account]
  (let [url (make-url "user" "expire")]
    (parse-http :delete url account)))
