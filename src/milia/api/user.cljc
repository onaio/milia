(ns milia.api.user
  (:refer-clojure :exclude [get update])
  (:require #?(:cljs [chimera.js-interop :refer [format]])
            [clojure.string :refer [join]]
            [chimera.seq :refer [has-keys?]]
            [chimera.core :refer [not-nil?]]
            [milia.api.http :refer [parse-http]]
            [milia.utils.remote :refer [make-url]]
            [milia.utils.retry :refer [retry-parse-http]]))

(defn patch
  [username params & {:keys [suppress-4xx-exceptions?]}]
  (let [url (make-url "profiles" (str username ".json"))
        options {#?(:clj :form-params
                    :cljs :json-params) params
                 #?(:clj :content-type) #?(:clj :json)}]
    (when username
      (parse-http :patch url :http-options options :as-map? true
                  :suppress-4xx-exceptions? suppress-4xx-exceptions?))))

(defn profile
  "Return the profile for the account username or the passed username."
  [username]
  {:pre [username]}
  (let [url (make-url "profiles" (str username ".json"))
        response (when username
                   (retry-parse-http :get url
                                     :suppress-4xx-exceptions? true
                                     :max-retries 4))]
    (if-let [error (:detail response)] nil response)))

(defn verify-email
  [verification-key]
  {:pre [verification-key]}
  (let [url (make-url "profiles"
                      (str "verify_email.json?verification_key="
                           verification-key))
        response (retry-parse-http :get url
                                   :suppress-4xx-exceptions? true
                                   :max-retries 2)]
    (if-let [error (:detail response)] nil response)))

(defn send-verification-email
  [username & [redirect-url]]
  {:pre [username]}
  (let [url (make-url "profiles" "send_verification_email.json")
        form-params
        (cond-> {:username username}
          (not-nil? redirect-url) (assoc :redirect_url redirect-url))]
    (parse-http :post url :http-options {:form-params form-params})))

(defn get-profiles-for-list-of-users
  "Return the profile for the account username or the passed username."
  [users]
  {:pre [(seq users)]}
  (let [url (make-url (str "profiles.json?users=" (join "," users)))
        response (parse-http :get url :suppress-4xx-exceptions? true)]
    (if-let [error (:detail response)] error response)))

(defn user
  "Return the user profile with authentication details."
  [& [suppress-4xx-exceptions?]]
  (let [url (make-url "user.json")]
    (parse-http :post url
                :suppress-4xx-exceptions? suppress-4xx-exceptions?)))

(defn get-subscription
  [username]
  (let [url (make-url "pricing" "subscriptions" username)]
    (parse-http :get url :suppress-4xx-exceptions? true)))

(defn update-subscription
  [username contactpersons]
  (let [url (make-url "pricing" "subscriptions" username)]
    (parse-http :patch url
                :http-options {:form-params {:contactpersons contactpersons}}
                :suppress-4xx-exceptions? true)))

(defn get-subscription-payment
  [username]
  (let [url (make-url "pricing" "payments" username)]
    (parse-http :get url)))

(defn get-invoices
  [username]
  (let [url (make-url "pricing" (str "invoices?username=" username))]
    (parse-http :get url)))

(defn create
  "Create a new user."
  [params]
  (let [profile (select-keys params [:first_name
                                     :last_name
                                     :username
                                     :email
                                     :password])
        url (make-url "profiles.json")]
    (parse-http :post url :http-options {:form-params profile})))

(defn all
  "return all users"
  []
  (let [url (make-url "users.json")] (parse-http :get url)))

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

  (let [url (make-url "profiles" (str username ".json"))]
    (when username
      (parse-http :put url
                  :http-options {:form-params params
                                 :content-type :json}
                  :as-map? true))))

(defn change-password
  "Change user password"
  [username current-password new-password]
  (let [url (make-url "profiles" username "change_password.json")
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
  [username metadata & {:keys [suppress-4xx-exceptions?]
                        :or {suppress-4xx-exceptions? nil}}]
  (let [current-metadata (retrieve-metadata username)
        updated-metadata (merge current-metadata metadata)]
    (patch username
           {:metadata updated-metadata}
           :suppress-4xx-exceptions? suppress-4xx-exceptions?)))

(defn get
  "Return the user for this username"
  [username]
  (let [url (make-url "users" (str username ".json"))]
    (parse-http :get url)))

(defn get-by-email
  "Return the users that match this email address"
  [email]
  (let [url (make-url "users.json")]
    (parse-http :get url
                :http-options {:query-params {:search email}}
                :suppress-4xx-exceptions? true)))

(defn generate-odk-token []
  (let [url (make-url "user" "odk_token.json")]
    (parse-http :post url)))

(defn trigger-password-reset-email
  "Trigger a password reset email to the given email and given return URL.
   Also takes an optional subject for the email message."
  ([email reset-url]
   (trigger-password-reset-email email reset-url nil))
  ([email reset-url reset-subject]
   (let [url (make-url "user" "reset.json")
         form-params (merge {:email email :reset_url reset-url}
                            (when reset-subject
                              {:email_subject reset-subject}))]
     ;; Unauthenticated API request does not need an account
     (parse-http :post url :http-options {:form-params form-params}))))

(defn reset-password
  [new-password token uid & {:keys [suppress-4xx-exceptions?]}]
  (let [url (make-url "user" "reset.json")]
    (parse-http :post url
                :suppress-4xx-exceptions? suppress-4xx-exceptions?
                :http-options {:form-params {:new_password new-password
                                             :token token
                                             :uid uid}})))

(defn change-email-address
  "Change the user's email address. This requires a password so that the API
   can successful update the authentication digest and email can be used to
   login."
  [username email-address password]
  (let [params {:email email-address :password password}]
    (patch username params :suppress-4xx-exceptions? true)))

(defn expire-temp-token
  "Expire the user's temporary token."
  []
  (let [url (make-url "user" "expire.json")]
    (parse-http :delete url)))

(defn google-sheet-authorization
  "Send a code to authorize a user to use google sheets"
  [code redirect_uri]
  (let [url (make-url "google"
                      (format "google_auth.json?code=%s&redirect_uri=%s"
                              code redirect_uri))]
    (parse-http :get url :as-map? true :suppress-4xx-exceptions? true)))
