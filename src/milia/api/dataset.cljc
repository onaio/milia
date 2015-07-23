(ns milia.api.dataset
  (:require #?(:clj [milia.api.io :refer [multipart-options]])
            [milia.api.http :refer [parse-http]]
            #?(:clj [milia.utils.file :as file-utils])
            [milia.utils.seq :refer [has-keys? in?]]
            [milia.utils.remote :refer [make-j2x-url make-client-url make-url]]))

(defn all
  "Return all the datasets for an account."
  [username]
  (let [url (make-url (str "forms?owner=" username))]
    (parse-http :get url)))

(defn public
  "Return all public datasets for a specific user."
  [username]
  (let [url (make-url "forms" username)]
    (parse-http :get url)))

#?(:clj
   (defn create
     "Create a new dataset from a file."
     ([upload]
      (create upload nil))
     ([upload project-id]
      (let [url (apply make-url (if project-id ["projects"
                                                project-id
                                                "forms"]
                                    ["forms"]))
            options (if-let [xls_file  (:xls_file upload)]
                      (multipart-options xls_file "xls_file")
                      {:form-params upload})]
        (parse-http :post url :http-options options)))))

#?(:clj
   (defn patch
     "Set the metadata for a dataset using PATCH. Only a subset of the
  required parameters are needed."
     ([dataset-id params]
      (patch dataset-id params nil))
     ([dataset-id params upload]
      (let [url (make-url "forms" dataset-id)
            options (if-let [xls_file (:xls_file upload)]
                      (multipart-options xls_file "xls_file")
                      {:form-params params})]
        (parse-http :patch url :http-options options)))))

(defn clone
  "Clone the dataset given by ID into the account with the given username."
  [dataset-id username]
  (let [url (make-url "forms" dataset-id "clone")
        data {:form-params {:username username}}]
    (parse-http :post url :http-options data :suppress-40x-exceptions? true )))

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
  (let [url (make-url "forms" dataset-id)]
    (parse-http :put url :http-options {:form-params params})))

(defn update-form-name
  "Update the title of a form"
  [dataset-id params]
  (let [url (make-url "forms" dataset-id)]
    (parse-http :put url {:form-params params})))

(defn data
  "Return the data associated with a dataset."
  [dataset-id & {:keys [:format :raw? :must-revalidate?]
                         #?@(:cljs [:or {:format "json"}])}]
  (let [dataset-suffix (if format (str dataset-id "." format) dataset-id)
        url (make-url "data" dataset-suffix)]
    (parse-http :get url :raw-response? raw? :must-revalidate? must-revalidate?)))

(defn record
  "Retrieve a record from the dataset."
  [dataset-id record-id]
  (let [url (make-url "data" dataset-id record-id)]
    (parse-http :get url)))

(defn tags
  "Returns tags for a dataset"
  [dataset-id]
  (let [url (make-url "forms" dataset-id "labels")]
    (parse-http :get url)))

(defn add-tags
  "Add tags to a dataset"
  [dataset-id tags]
    (let [url (make-url "forms" dataset-id "labels")]
    (parse-http :post url :http-options {:form-params tags})))

(defn filename-for-format
  "Return filename taking format special cases into account."
  [dataset-id format]
  (str dataset-id "." (if (= format "csvzip") "zip" format)))

(defn- options-for-format
  "Return options needed to handle format."
  [format]
  (if (in? ["csvzip" "sav" "xls" "xlsx"] format) {:as :byte-array} {}))

#?(:clj
   (defn download
     "Download dataset in specified format."
     ([dataset-id format]
      (download dataset-id format false))
     ([dataset-id format async]
      (download dataset-id format async false))
     ([dataset-id format async dataview]
      (let [path (str dataset-id "." format)
            options (options-for-format format)
            url (if dataview
                  (make-url "dataviews" dataset-id (str "data." format))
                  (make-url (if async "forms" "data") path))
            filename (filename-for-format dataset-id format)]
        (parse-http :get url :http-options options :filename filename)))))

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
  [dataset-id]
  (let [url (make-url "forms" (str dataset-id ".json"))]
    (parse-http :get url)))

(defn online-data-entry-link
  "Return link to online data entry."
  [dataset-id]
  (let [url (make-url "forms" dataset-id "enketo")]
    #?(:clj
       (:enketo_url
         (parse-http :get url :suppress-40x-exceptions? true))
       :cljs
       (parse-http :get url))))

(defn edit-link
  "Return link to online data entry."
  [username project-id dataset-id instance-id]
  (let [return-url (make-client-url username project-id dataset-id "submission-editing-complete")
        url (make-url "data" dataset-id instance-id
                      (str "enketo?return_url=" return-url))]
    (:url (parse-http :get url))))

(defn delete
  "Delete a dataset by ID."
  [dataset-id]
  (let [url (make-url "forms" dataset-id "delete_async")]
    (parse-http :delete url)))

(defn move-to-project
  "Move a dataset to a project use account if no owner passed."
  [dataset-id project-id]
  (let [url (make-url "projects" project-id "forms")]
    (parse-http :post url :http-options {:form-params {:formid dataset-id}})))

(defn update-sharing
  "Share dataset with specific user"
  [dataset-id username role]
  (let [url (make-url "forms" dataset-id "share")
        data {:username username :role role}]
    (parse-http :post url :http-options {:form-params data})))

#?(:clj
   (defn upload-media
     "Upload media for a form"
     [datasetd-id media-file]
     (let [url (make-url "metadata")
           data-file (file-utils/uploaded->file media-file)
           muiltipart [{:name "data_value"
                        :content (:filename media-file)}
                       {:name "data_type"
                        :content "media"}
                       {:name "xform"
                        :content datasetd-id}
                       {:name "data_file"
                        :content data-file}]]
       (parse-http :post url :http-options {:multipart muiltipart}))))

(defn add-xls-report
  "Add xls report link to dataset"
  [dataset-id uuid filename]
  (let [xls-url (make-j2x-url "xls" uuid)
        url (make-url "metadata")
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
                   (str dataset-id ".xls?meta=" meta-id "&data_id="data-id)
                   (str dataset-id ".xls?meta=" meta-id))
          url (make-url "forms" suffix)]
      (parse-http :get
                  url
                  :http-options {:as :byte-array}
                  :as-map? true
                  :filename filename))))

#?(:clj
   (defn csv-import
     "Upload CSV data to existing form"
     [dataset-id media-file]
     (let [url (make-url "forms" dataset-id "csv_import")
           multipart (multipart-options media-file "csv_file")]
       (parse-http :post url :http-options multipart))))
