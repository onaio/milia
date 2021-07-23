(ns milia.api.dataset
  #? (:clj (:import [java.net URLEncoder]))
  (:refer-clojure :exclude [clone update])
  (:require [chimera.seq :refer [has-keys? in?]]
            [chimera.core :refer [not-nil?]]
            [chimera.string :refer [get-query-params-str]]
            [clojure.string :refer [join split]]
            [milia.api.http :refer [parse-http]]
            [milia.utils.metadata :refer [metadata-files]]
            [milia.utils.remote
             :refer [make-j2x-url make-client-url make-url]]
            #?@(:clj [[milia.api.io :refer [multipart-options]]
                      [milia.utils.file :as file-utils]
                      [milia.utils.metadata :refer [upload-metadata-file]]
                      [cheshire.core :refer [generate-string]]])))

(defmulti type->endpoint (fn [datatype & _] datatype))
(defmethod type->endpoint :default [_ & {:keys [async] :or {async true}}]
  (if async "forms" "data"))
(defmethod type->endpoint :filtered-dataset [_ & _] "dataviews")

(defn all
  "Return all the datasets for an account."
  [username]
  (let [url (make-url (str "forms.json?owner=" username))]
    (parse-http :get url)))

(defn public
  "Return all public datasets for a specific user."
  [username]
  (let [url (make-url "forms" (str username ".json"))]
    (parse-http :get url)))

#?(:clj
   (defn- send-file-or-params
     "Send request with file or params"
     [method
      url
      {:keys [xls_file xml_file] :as params}
      suppress-4xx-exceptions?]
     (let [options
           (cond
             (not-nil? xls_file) (multipart-options xls_file "xls_file")
             (not-nil? xml_file) (multipart-options xml_file "xml_file")
             :else {:form-params params})]
       (parse-http method url :http-options options
                   :suppress-4xx-exceptions? suppress-4xx-exceptions?))))
#?(:clj
   (defn create
     "Create a new dataset from a file."
     ([params]
      (create params nil))
     ([params project-id]
      (let [url (apply make-url (if project-id
                                  ["projects" project-id "forms.json"]
                                  ["forms.json"]))]
        (send-file-or-params :post url params false)))))

#?(:clj
   (defn patch
     "Set the metadata for a dataset using PATCH. Only a subset of the
      required parameters are needed."
     [dataset-id params & {:keys [suppress-4xx-exceptions?]
                           :or {suppress-4xx-exceptions? true}}]
     (let [url (make-url "forms" (str dataset-id ".json"))]
       (send-file-or-params :patch url params suppress-4xx-exceptions?))))

(defn clone
  "Clone the dataset given by ID into the account with the given username."
  [dataset-id username & {:keys [project-id]}]
  (let [url (make-url "forms" dataset-id "clone.json")
        data-base {:form-params {:username username}}
        data (if project-id
               (assoc-in data-base [:form-params :project_id] project-id)
               data-base)]
    (parse-http :post url :http-options data :suppress-4xx-exceptions? true)))

(defn update
  "Set the metadata for a dataset using PUT. All parameters must be passed."
  [dataset-id params]
  {:pre [(has-keys? params [:created_by
                            :description
                            :downloadable
                            :owner
                            :project
                            :public
                            :public_data
                            :title
                            :uuid])]}
  (let [url (make-url "forms" (str dataset-id ".json"))]
    (parse-http :put url :http-options {:form-params params})))

(defn update-form-name
  "Update the title of a form"
  [dataset-id params]
  (let [url (make-url "forms" dataset-id)]
    (parse-http :put url :http-options {:form-params params})))

(defn ^:export data
  "Return the data associated with a dataset."
  [dataset-id &
   {:keys [format raw? must-revalidate? accept-header query-params
           data-id auth-token] #?@(:cljs [:or {:format "json"}])}]
  (let [dataset-suffix (if format
                         (str dataset-id (when data-id (str "/" data-id))
                              "." format)
                         dataset-id)
        url (make-url "data" dataset-suffix)
        options {:query-params query-params}]
    (parse-http :get url
                :http-options options
                :raw-response? raw?
                :must-revalidate? must-revalidate?
                :accept-header accept-header
                :auth-token auth-token)))

(defn record
  "Retrieve a record from the dataset."
  [dataset-id record-id]
  (let [url (make-url "data" dataset-id (str record-id ".json"))]
    (parse-http :get url)))

(defn tags
  "Returns tags for a dataset"
  [dataset-id]
  (let [url (make-url "forms" dataset-id "labels.json")]
    (parse-http :get url)))

(defn add-tags
  "Add tags to a dataset"
  [dataset-id tags]
  (let [url (make-url "forms" dataset-id "labels.json")]
    (parse-http :post url :http-options {:form-params tags})))

(defn filename-for-format
  "Return filename taking format special cases into account."
  [dataset-id format]
  (str dataset-id "." (if (= format "csvzip") "zip" format)))

(defn- options-for-format
  "Return options needed to handle format."
  [format]
  (if (in? ["csvzip" "sav" "xls" "xlsx" "zip"] format) {:as :byte-array} {}))

(defmulti type->download-path (fn [datatype & _] datatype))

(defmethod type->download-path :default
  [_ dataset-id format export-options]
  [(str dataset-id "." format
        (when export-options
          (str "?"
               (join "&"
                     (for [[option val] export-options]
                       (str (name option) "="
                            #?(:clj (URLEncoder/encode val)
                               :cljs val)))))))])

(defmethod type->download-path :filtered-dataset
  [_ dataset-id format export-options]
  [dataset-id (str "data." format)])

#?(:clj
   (defn download
     "Download dataset in specified format."
     [dataset-id format & [async data-type export-options]]
     (let [options (options-for-format format)
           endpoint (type->endpoint data-type :async async)
           url (apply make-url
                      (cons endpoint
                            (type->download-path data-type
                                                 dataset-id
                                                 format
                                                 export-options)))
           filename (filename-for-format dataset-id format)]
       (parse-http :get url :http-options options :filename filename))))

(defn download-synchronously
  "Download form data in specified format. The synchronicity here refers to the
   server side. This will still return a channel, not data, in CLJS.
   The options map (last parameter) has the following keys:
   :accept-header Defaults to application/json
   :submission-id The id of the submission whose data the client requires. The
    function returns data for all submissions if this is not provided.
   :dataview? Boolean flag indicating whether the data belongs to a filtered
    dataview"
  [dataset-id format
   & {:keys [accept-header submission-id dataview?]}]
  (let [url (cond
              dataview? (make-url "dataviews" dataset-id (str "data." format))
              submission-id (make-url "data"
                                      dataset-id (str submission-id "." format))
              :default (make-url "data" (str dataset-id "." format)))]
    (parse-http :get url
                :accept-header accept-header
                :http-options (options-for-format format))))

(defn form
  "Download form as JSON string or file in specified format if format passed."
  ([dataset-id]
   (let [url (make-url "forms" dataset-id "form.json")]
     (parse-http :get url)))
  ([dataset-id format]
   (let [suffix (str "form." format)
         options (options-for-format format)
         url (make-url "forms" dataset-id suffix)
         filename (str dataset-id "_" suffix)]
     (parse-http :get url :http-options options :filename filename))))

(defn metadata
  "Show dataset metadata."
  [dataset-id & {:keys [no-cache?]}]
  (let [url (make-url "forms" (str dataset-id ".json"))]
    (parse-http :get url :no-cache? no-cache?)))

(defn online-data-entry-link
  "Return link to online data entry."
  [dataset-id]
  (let [url (make-url "forms" dataset-id "enketo.json")]
    #?(:clj
       (parse-http :get url :suppress-4xx-exceptions? true)
       :cljs
       (parse-http :get url))))

(defn edit-link
  "Return link to online data entry."
  [username project-id dataset-id instance-id]
  (let [return-url (make-client-url username
                                    project-id
                                    dataset-id
                                    "submission-editing-complete")
        url (make-url "data" dataset-id instance-id
                      (str "enketo.json?return_url=" return-url))]
    (:url (parse-http :get url))))

(defn delete
  "Delete a dataset by ID."
  [dataset-id]
  (let [url (make-url "forms" dataset-id "delete_async.json")]
    (parse-http :delete url)))

(defn move-to-project
  "Move a dataset to a project use account if no owner passed."
  [dataset-id project-id]
  (let [url (make-url "projects" project-id "forms.json")]
    (parse-http :post url :http-options {:form-params {:formid dataset-id}})))

(defn new-form-owner
  "Set a new form owner"
  [dataset-id new-owner]
  (let [url (make-url "forms" (str dataset-id ".json"))
        new-owner (make-url "users" new-owner)]
    (parse-http :patch url :http-options {:form-params {:owner new-owner}})))

(defn update-sharing
  "Share dataset with specific user"
  [dataset-id username role]
  (let [url (make-url "forms" dataset-id "share.json")
        data {:username username :role role}]
    (parse-http :post url :http-options {:form-params data})))

#?(:clj
   (defn upload-media
     "Upload media for a form"
     [datasetd-id media-file]
     (let [url (make-url "metadata.json")
           data-file (file-utils/uploaded->file media-file)
           muiltipart [{:name "data_value"
                        :content (:filename media-file)}
                       {:name "data_type"
                        :content "media"}
                       {:name "xform"
                        :content datasetd-id}
                       {:name "data_file"
                        :content data-file}]]
       (parse-http :post url
                   :http-options {:multipart muiltipart}
                   :suppress-4xx-exceptions? true))))

(defn link-xform-or-dataview-as-media
  "Link xform or dataview as media"
  [object-type object-id media-filename xform-id]
  (let [url (make-url "metadata.json")
        form-params {:data_type "media"
                     :data_value
                     (str (join " " [object-type object-id media-filename]))
                     :xform xform-id}]
    (parse-http :post url
                :http-options {:form-params form-params}
                :suppress-4xx-exceptions? true)))

(defn add-xls-report
  "Add xls report link to dataset"
  [dataset-id uuid filename]
  (let [xls-url (make-j2x-url "xls" uuid)
        url (make-url "metadata.json")
        data {:xform dataset-id
              :data_type "external_export"
              :data_value (str filename "|" xls-url)}]
    (parse-http :post url :http-options {:form-params data})))

(defn download-xls-report
  "Download xls report from the j2x service"
  ([dataset-id meta-id filename]
   (download-xls-report dataset-id meta-id filename nil))
  ([dataset-id meta-id filename data-id]
   (let [suffix (if data-id
                  (str dataset-id ".xls?meta=" meta-id "&data_id=" data-id)
                  (str dataset-id ".xls?meta=" meta-id))
         url (make-url "forms" suffix)]
     (parse-http :get
                 url
                 :http-options {:as :byte-array}
                 :as-map? true
                 :filename filename))))

(def file-ext-query-param-map {:csv "csv_file"
                               :xls "xls_file"
                               :xlsx "xls_file"})

(defn get-media-file-extension [filename]
  (-> filename
      (split #"\.")
      peek))

#?(:clj
   (defn file-import
     "Import csv or xls file data to existing form"
     [dataset-id {:keys [filename] :as media-file} & [overwrite?]]
     (let [media-file-extension (get-media-file-extension filename)
           url (make-url "forms"
                         dataset-id
                         (cond-> "import.json"
                           overwrite? (str "?overwrite=true")))
           multipart (multipart-options media-file
                                        (->> media-file-extension
                                             keyword
                                             (get file-ext-query-param-map)))]
       (parse-http :post url :http-options multipart
                   :suppress-4xx-exceptions? true
                   :as-map? true))))

(defn edit-history
  "Returns a submission's edit history"
  [dataset-id instance-id]
  (parse-http :get (make-url "data" dataset-id instance-id "history.json")))

#?(:clj
   (defn upload-file
     "Upload metadata file for a submission"
     [submission-id file]
     (upload-metadata-file "instance" submission-id file)))

(defn files
  [instance-id project-id & {:keys [no-cache? dataset-id]}]
  (let [extra-params (apply assoc {:project project-id}
                            [:xform dataset-id])]
    (metadata-files :instance instance-id no-cache?
                    :extra-params extra-params)))

(defn update-xform-meta-permissions
  "Integer Integer String String -> Channel HttpResponse"
  [dataset-id metadata-id editor-meta-role dataentry-meta-role]
  (parse-http
   :put (make-url "metadata" (str metadata-id ".json"))
   :http-options
   {:form-params
    {:data_type  "xform_meta_perms"
     :xform      dataset-id
     :data_value (str editor-meta-role "|" dataentry-meta-role)}}))

(defn create-xform-meta-permissions
  "Integer String String -> Channel HttpResponse"
  [dataset-id editor-meta-role dataentry-meta-role]
  (parse-http
   :post (make-url "metadata.json")
   :http-options
   {:form-params
    {:data_type  "xform_meta_perms"
     :xform      dataset-id
     :data_value (str editor-meta-role "|" dataentry-meta-role)}}))

(defn create-submission-review
  "Create a submission review"
  [{:keys [status instance note]}]
  (parse-http
   :post (make-url "submissionreview.json")
   :http-options
   {:form-params
    {:status status
     :instance instance
     :note note}}))

(defn create-multiple-submission-reviews
  "Create a submission review"
  [{:keys [status instances note]}]
  (let [json-vec (mapv (fn [instance]
                         {:note note :status status :instance instance})
                       instances)]
    (parse-http
     :post (make-url "submissionreview.json")
     :http-options
     #?(:clj {:body
              (generate-string json-vec)
              :content-type :json})
     #?(:cljs {:json-params json-vec}))))

(defn get-submission-review
  "Get a submission review"
  [submission-review-id]
  (parse-http :get (make-url "submissionreview"
                             (str submission-review-id ".json"))))

(defn list-submission-reviews
  "List a submission review"
  []
  (parse-http :get (make-url "submissionreview.json")))

(defn update-submission-review
  "Update a submission review"
  [{:keys [submission-review-id status note]}]
  (when (not-every? nil? [status note])
    (parse-http
     :patch (make-url "submissionreview" (str submission-review-id ".json"))
     :http-options
     {:form-params
      (cond-> {}
        (not-nil? status) (assoc :status status)
        (not-nil? note) (assoc :note note))})))

(defn filter-submission-review-by-instance
  [{:keys [instance status note]}]
  (let [query-params-str
        (get-query-params-str
         (cond-> {}
           (not-nil? instance) (assoc :instance instance)
           (not-nil? status) (assoc :status status)
           (not-nil? note) (assoc :note note)))]
    (parse-http
     :get (make-url (str "submissionreview.json" query-params-str)))))

(defn delete-submission-review
  [instance]
  (parse-http :delete (make-url "submissionreview" (str instance ".json"))))
