(ns milia.api.user
  (:require [milia.api.http :refer [parse-http]]
            [milia.utils.remote :refer [make-url]]
            [milia.utils.seq :refer [has-keys?]]))

(defn patch
  [username params & {:keys [suppress-4xx-exceptions?]}]
  (let [url (make-url "profiles" username)
        options {#?(:clj :form-params
                    :cljs :json-params) params
                 :content-type :json}]
    (parse-http :patch url :http-options options :as-map? true
                :suppress-4xx-exceptions? suppress-4xx-exceptions?)))

(defn profile
  "Return the profile for the account username or the passed username."
  [username]
  (let [url (make-url "profiles" username)
        response (parse-http :get url :suppress-4xx-exceptions? true)]
    (if-let [error (:detail response)] nil response)))

(defn user
  "Return the user profile with authentication details."
  ([]
   (user false))
  ([suppress-4xx-exceptions?]
   (let [url (make-url "user")]
     (parse-http :get url
                 :suppress-4xx-exceptions? suppress-4xx-exceptions?))))

(defn create
  "Create a new user."
  [params]
  (let [profile (select-keys params [:first_name
                                     :last_name
                                     :username
                                     :email
                                     :password])
        url (make-url "profiles")]
    (parse-http :post url :http-options {:form-params profile})))

(defn all
  "return all users"
  []
  (let [url (make-url "users")] (parse-http :get url)))

(defn update
  "update user profile"
  [username params]
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
  (let [url (make-url "profiles" username)]
    (parse-http :put url
                :http-options {:form-params params
                               :content-type :json}
                :as-map? true)))

(defn change-password
  "Change user password"
  [username current-password new-password]
  (let [url (make-url "profiles" username "change_password")
        options {:form-params {:current_password current-password
                               :new_password new-password}}]
    (parse-http :post url :http-options options
                :raw-response? true
                :suppress-4xx-exceptions? true
                :as-map? true)))

(defn retrieve-metadata
  [username]
  (:metadata (profile username)))

(defn update-user-metadata
  [username metadata]
  (let [current-metadata (retrieve-metadata username)
        updated-metadata (merge current-metadata metadata)]
    (patch username {:metadata updated-metadata})))

(defn get-by-email
  "Return the users that match this email address"
  [email]
  (let [url (make-url "users")]
    (parse-http :get url
                :http-options {:query-params {:search email}}
                :suppress-4xx-exceptions? true)))

(defn trigger-password-reset-email
  "Trigger a password reset email to the given email and given return URL.
   Also takes an optional subject for the email message."
  ([email reset-url]
   (trigger-password-reset-email email reset-url nil))
  ([email reset-url reset-subject]
   (let [url (make-url "user" "reset")
         form-params (merge {:email email :reset_url reset-url}
                            (when reset-subject
                              {:email_subject reset-subject}))]
     ;; Unauthenticated API request does not need an account
     (parse-http :post url :http-options {:form-params form-params}))))

(defn reset-password
  [new-password token uid]
  (let [url (make-url "user" "reset")]
    (parse-http :post url
                :http-options {:form-params {:new_password new-password
                                             :token token
                                             :uid uid}})))

(defn change-email-address
  "Change the user's email address"
  [username email-address]
  (let [params {:email email-address}]
    (patch username params :suppress-4xx-exceptions? true)))

(defn expire-temp-token
  "Expire the user's temporary token."
  []
  (let [url (make-url "user" "expire")]
    (parse-http :delete url)))