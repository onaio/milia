(ns milia.api.dataset-test
;;   (:refer-clojure :exclude [update])
  (:require [cheshire.core :refer [generate-string]]
            [clojure.string :refer [join]]
            [midje.sweet :refer :all]
            [milia.api.dataset :refer :all]
            [milia.utils.file :as f]
            [milia.utils.remote :refer [make-j2x-url
                                        make-url
                                        make-client-url]]
            [milia.api.http :refer [parse-http]]
            [milia.api.io :refer [multipart-options]]))

(def dataset-id 123)
(def dataset-id-url (str dataset-id ".json"))

(let [url :fake-url
      username :fake-username
      password :fake-password
      params {:created_by :created_by
              :description :description
              :downloadable :downloadable
              :owner :owner
              :project :project
              :public :public
              :public_data :public_data
              :title :title
              :uuid :uuid
              :version :version}]
  (facts "about datasets"
         (fact "Should get correct url"
               (let [username "bob"]
                 (all username) => :something
                 (provided
                  (make-url "forms.json?owner=bob") => url
                  (parse-http :get url) => :something))))

  (facts "about datasets-update"
         (fact "Should get correct url"
               (update dataset-id params) => :something
               (provided
                (make-url "forms" dataset-id-url) => url
                (parse-http :put url
                            :http-options {:form-params params})
                => :something)))

  (facts "about dataset metadata"
         (fact "should get dataset metadata"
               (metadata dataset-id) => :fake-metadata
               (provided
                (make-url "forms" dataset-id-url) => url
                (parse-http :get url :no-cache? nil) => :fake-metadata)))

  (facts "about dataset metadata with cache"
         (fact "should get dataset metadata and show caching was true"
               (metadata dataset-id :no-cache? true) => :fake-metadata-cache
               (provided
                (make-url "forms" dataset-id-url) => url
                (parse-http :get url :no-cache? true)
                => :fake-metadata-cache)))

  (fact "about dataset-getdata"
        (data dataset-id) => :something
        (provided
         (make-url "data" dataset-id) => url
         (parse-http :get
                     url
                     :http-options {:query-params nil}
                     :raw-response? nil
                     :must-revalidate? nil
                     :accept-header nil
                     :auth-token nil) => :something))

  (fact "about dataset get dataset datum"
        (data dataset-id :datum-id 1 :format "json") => :something
        (provided
         (make-url "data"  dataset-id-url) => url
         (parse-http :get
                     url
                     :http-options {:query-params nil}
                     :raw-response? nil
                     :must-revalidate? nil
                     :accept-header nil
                     :auth-token nil) => :something))

  (fact "about dataset-getdata :raw"
        (data dataset-id :raw? true :must-revalidate? true) => :something
        (provided
         (make-url "data" dataset-id) => url
         (parse-http :get
                     url
                     :http-options {:query-params nil}
                     :raw-response? true
                     :must-revalidate? true
                     :accept-header nil
                     :auth-token nil) => :something))

  (fact "about dataset-getdata with :accept-header"
        (data dataset-id :accept-header "text/*"
              :must-revalidate? true) => :something
        (provided
         (make-url "data" dataset-id) => url
         (parse-http :get
                     url
                     :http-options {:query-params nil}
                     :raw-response? nil
                     :must-revalidate? true
                     :accept-header "text/*"
                     :auth-token nil) => :something))

  (fact "about dataset-getdata with :query"
        (let [query (str "{\"_submitted_by\":\"" username "\"}")]
          (data dataset-id :query-params {:query query}) => :something
          (provided
           (make-url "data" dataset-id) => url
           (parse-http :get
                       url
                       :http-options {:query-params {:query query}}
                       :raw-response? nil
                       :must-revalidate? nil
                       :accept-header nil
                       :auth-token nil) => :something)))

  (fact "about dataset-getrecord"
        (record dataset-id :record-id) => :something
        (provided
         (make-url "data" 123 ":record-id.json") => url
         (parse-http :get url) => :something))

  (fact "about dataset-get-tags"
        (tags dataset-id) => :something
        (provided
         (make-url "forms" dataset-id "labels.json") => url
         (parse-http :get url) => :something))

  (fact "about dataset-add-tag"
        (add-tags dataset-id :tags) => :something
        (provided
         (make-url "forms" dataset-id "labels.json") => url
         (parse-http :post url
                     :http-options {:form-params :tags}) => :something))

  (facts "About dataset download"
         (fact "Should make data URL and parse response"
               (let [format "csv"
                     filename (str dataset-id "." format)]
                 (download dataset-id format) => :fake-file
                 (provided
                  (make-url "data" filename) => url
                  (parse-http :get url :http-options {}
                              :filename filename) => :fake-file)))

         (fact "Should change URL for async"
               (let [format "csv"
                     filename (str dataset-id "." format)]
                 (download dataset-id format true) => :fake-file
                 (provided
                  (make-url "forms" filename) => url
                  (parse-http :get url :http-options {}
                              :filename filename) => :fake-file)))

         (fact "Should handle XLS as byte array"
               (let [format "xls"
                     filename (str dataset-id "." format)]
                 (download dataset-id format) => :fake-file
                 (provided
                  (make-url "data" filename) => url
                  (parse-http :get
                              url
                              :http-options {:as :byte-array}
                              :filename filename) => :fake-file)))

         (fact "Should handle csvzip zip extension"
               (let [format "csvzip"
                     path (str dataset-id "." format)
                     filename (str dataset-id ".zip")]
                 (download dataset-id format) => :fake-file
                 (provided
                  (make-url "data" path) => url
                  (parse-http :get url :http-options {:as :byte-array}
                              :filename filename) => :fake-file)))

         (fact "Should add extra download options to request"
               (let [format "csv"
                     filename (str dataset-id "." format)
                     uri (str dataset-id "." format
                              "?remove-group-names=true&group-delimiter=/")
                     export-options {:remove-group-names true
                                     :group-delimiter "/"}]
                 (download dataset-id format true false export-options)
                 => :fake-file
                 (provided
                  (make-url "forms" uri) => url
                  (parse-http :get url :http-options {}
                              :filename filename) => :fake-file))))

  (facts "about download-synchronously"
         (let [format "leet"
               accept-header "text/leet"
               dataset-id 1337
               submission-id 42
               form-data-url (make-url "data" (str dataset-id "." format))
               form-data-url-with-submission-id
               (make-url "data" dataset-id (str submission-id
                                                "."
                                                format))
               dataview-data-url (make-url "dataviews"
                                           dataset-id (str "data." format))]
           (fact "calls parse-http with the correct parameters for forms"
                 (download-synchronously dataset-id format
                                         :accept-header accept-header)
                 => :response
                 (provided
                  (parse-http :get form-data-url
                              :accept-header accept-header
                              :http-options {}) => :response))
           (fact
            "calls parse-http with the correct parameters for forms given a
                  submission-id"
            (download-synchronously dataset-id format
                                    :accept-header accept-header
                                    :submission-id submission-id)
            => :response
            (provided
             (parse-http :get form-data-url-with-submission-id
                         :accept-header accept-header
                         :http-options {}) => :response))
           (fact "calls parse-http with the correct parameters for filtered
                  dataview"
                 (download-synchronously dataset-id format
                                         :accept-header accept-header
                                         :dataview? true)
                 => :response
                 (provided
                  (parse-http :get dataview-data-url
                              :accept-header accept-header
                              :http-options {}) => :response))))

  (facts "about dataset form"
         (fact "Return JSON string"
               (form dataset-id) => :json
               (provided
                (make-url "forms" dataset-id "form.json") => url
                (parse-http :get url) => :json))

         (fact "Download as format"
               (let [format "csv"
                     suffix (str "form." format)
                     filename (str dataset-id "_" suffix)]
                 (form dataset-id format) => :fake-file
                 (provided
                  (make-url "forms" dataset-id suffix) => url
                  (parse-http :get url :http-options {}
                              :filename filename) => :fake-file))))

  (fact "about online-data-entry-link"
        (online-data-entry-link dataset-id) => {:enketo_url :enketo_url}
        (provided
         (make-url "forms" dataset-id "enketo.json") => url
         (#'milia.api.io/http-request :get url nil) =>
         {:body :body
          :request :request
          :status 200}
         (milia.api.io/parse-response :body
                                      200
                                      nil
                                      nil) => {:enketo_url :enketo_url}))

  (fact "about dataset delete"
        (delete dataset-id) => :response
        (provided
         (make-url "forms" dataset-id "delete_async.json") => url
         (parse-http :delete url) => :response))

  (fact "about create dataset"
        (let [options {:multipart [{:name "xls_file"
                                    :content :xlsfile}]}]
          (create {:xls_file :uploaded-file}) => :response
          (provided
           (multipart-options :uploaded-file "xls_file") => options
           (make-url "forms.json") => url
           (parse-http :post
                       url
                       :http-options options
                       :suppress-4xx-exceptions? false) => :response)))

  (fact "about move dataset to project"
        (move-to-project 1 :project-id) => :form
        (provided
         (make-url "projects" :project-id "forms.json") => url
         (parse-http :post
                     url
                     :http-options {:form-params {:formid 1}}) => :form))

  (fact "should change form owner"
        (new-form-owner :id :new_owner) => :response
        (provided
         (make-url "forms" ":id.json") => url
         (make-url "users" :new_owner) => :user-url
         (parse-http :patch
                     url
                     :http-options {:form-params {:owner :user-url}})
         => :response))

  (facts "about update-sharing for dataset"
         (let [username :fake-username
               role :fake-role
               data {:username username :role role}]
           (fact "Should return result of parse-http"
                 (update-sharing dataset-id
                                 username
                                 role) => :sharing-updated
                 (provided
                  (make-url "forms" dataset-id "share.json") => url
                  (parse-http :post url :http-options
                              {:form-params data})
                  => :sharing-updated))))

  (fact "about upload media"
        (upload-media dataset-id {:filename "image.png"}) => :response
        (provided
         (f/uploaded->file {:filename "image.png"}) => :media-file
         (make-url "metadata.json") => url
         (parse-http :post
                     url
                     :http-options {:multipart [{:name "data_value"
                                                 :content "image.png"}
                                                {:name "data_type"
                                                 :content "media"}
                                                {:name "xform"
                                                 :content dataset-id}
                                                {:name "data_file"
                                                 :content :media-file}]}
                     :suppress-4xx-exceptions? true)
         => :response))

  (fact "about link xform or dataview as media"
        (link-xform-or-dataview-as-media
         "dataview" "123" "dataview-example" "45") => :response
        (provided
         (make-url "metadata.json") => url
         (parse-http :post
                     url
                     :http-options {:form-params {:data_type "media"
                                                  :data_value
                                                  (str
                                                   (join
                                                    " "
                                                    ["dataview"
                                                     "123"
                                                     "dataview-example"]))
                                                  :xform "45"}}
                     :suppress-4xx-exceptions? true)
         => :response))

  (facts "About patch"
         (let [options {:form-params nil}
               file {:xls_file :uploaded-file}
               multipart-options-map {:multi :part}
               dataset-id-url "123.json"]
           (fact "Should call parse-http with patch"
                 (patch dataset-id nil) => :response
                 (provided
                  (make-url "forms" dataset-id-url) => url
                  (parse-http :patch
                              url
                              :http-options options
                              :suppress-4xx-exceptions? true) => :response))
           (comment

             (fact "Should call parse-http with multipart options"
                   (patch dataset-id file) => :response
                   (provided
                    (make-url "forms" dataset-id-url) => url
                    (multipart-options :uploaded-file "xls_file")
                    => multipart-options-map
                    (parse-http :patch
                                url
                                :http-options multipart-options-map
                                :suppress-4xx-exceptions? true) => :response))

             (fact "Should respect suppress option"
                   (patch dataset-id file :suppress-4xx-exceptions? false)
                   => :response
                   (provided
                    (make-url "forms" dataset-id-url) => url
                    (multipart-options :uploaded-file "xls_file")
                    => multipart-options-map
                    (parse-http
                     :patch
                     url
                     :http-options multipart-options-map
                     :suppress-4xx-exceptions? false) => :response)))))

  (facts "about xls template reports"
         (let [media-file {:filename "filename"}
               uuid "12345621"
               add-xls-response {:status 200
                                 :body :xls-metadata}
               byte-array [0,1]
               data-value (str ":filename|" (make-j2x-url "xls" ":uuid"))]
           (fact "Should add xls report to Ona"
                 (add-xls-report dataset-id
                                 :uuid
                                 :filename)
                 => (contains add-xls-response)
                 (provided
                  (make-url "metadata.json") => url
                  (parse-http :post url
                              :http-options {:form-params
                                             {:xform dataset-id
                                              :data_type "external_export"
                                              :data_value data-value}})
                  => add-xls-response))))

  (facts "About File Imports"
         (fact "should import csv file to dataset endpoint when overwrite? is
                true"
               (let [multipart-options-map {:multi :part}
                     media-file {:filename "file.csv"}]
                 (file-import dataset-id media-file true) => :response
                 (provided
                  (get-media-file-extension "file.csv") => :csv
                  (make-url "forms"
                            dataset-id
                            "import.json?overwrite=true") => url
                  (multipart-options media-file "csv_file")
                  => multipart-options-map
                  (parse-http :post :fake-url
                              :http-options multipart-options-map
                              :suppress-4xx-exceptions? true
                              :as-map? true)
                  => :response)))
         (fact "should import csv file to dataset endpoint when overwrite? is
                false"
               (let [multipart-options-map {:multi :part}
                     media-file {:filename "file.csv"}]
                 (file-import dataset-id media-file) => :response
                 (provided
                  (get-media-file-extension "file.csv") => :csv
                  (make-url "forms" dataset-id "import.json") => url
                  (multipart-options media-file "csv_file")
                  => multipart-options-map
                  (parse-http :post :fake-url
                              :http-options multipart-options-map
                              :suppress-4xx-exceptions? true
                              :as-map? true)
                  => :response)))
         (fact "should import xls file to dataset endpoint when overwrite? is
                true"
               (let [multipart-options-map {:multi :part}
                     media-file {:filename "file.xls"}]
                 (file-import dataset-id media-file true) => :response
                 (provided
                  (get-media-file-extension "file.xls") => :xls
                  (make-url "forms"
                            dataset-id
                            "import.json?overwrite=true") => url
                  (multipart-options media-file "xls_file")
                  => multipart-options-map
                  (parse-http :post :fake-url
                              :http-options multipart-options-map
                              :suppress-4xx-exceptions? true
                              :as-map? true)
                  => :response)))
         (fact "should import xls file to dataset endpoint when overwrite? is
                false"
               (let [multipart-options-map {:multi :part}
                     media-file {:filename "file.xls"}]
                 (file-import dataset-id media-file) => :response
                 (provided
                  (get-media-file-extension "file.xls") => :xls
                  (make-url "forms" dataset-id "import.json") => url
                  (multipart-options media-file "xls_file")
                  => multipart-options-map
                  (parse-http :post :fake-url
                              :http-options multipart-options-map
                              :suppress-4xx-exceptions? true
                              :as-map? true)
                  => :response)))
         (fact "should import xlsx file to dataset endpoint when overwrite? is
                true"
               (let [multipart-options-map {:multi :part}
                     media-file {:filename "file.xlsx"}]
                 (file-import dataset-id media-file true) => :response
                 (provided
                  (get-media-file-extension "file.xlsx") => :xlsx
                  (make-url "forms"
                            dataset-id
                            "import.json?overwrite=true") => url
                  (multipart-options media-file "xls_file")
                  => multipart-options-map
                  (parse-http :post :fake-url
                              :http-options multipart-options-map
                              :suppress-4xx-exceptions? true
                              :as-map? true)
                  => :response)))
         (fact "should import xlsx file to dataset endpoint when overwrite? is
                false"
               (let [multipart-options-map {:multi :part}
                     media-file {:filename "file.xlsx"}]
                 (file-import dataset-id media-file) => :response
                 (provided
                  (get-media-file-extension "file.xlsx") => :xlsx
                  (make-url "forms" dataset-id "import.json") => url
                  (multipart-options media-file "xls_file")
                  => multipart-options-map
                  (parse-http :post :fake-url
                              :http-options multipart-options-map
                              :suppress-4xx-exceptions? true
                              :as-map? true)
                  => :response))))

  (fact "Should download xls report"
        (let [form-xls-url (str dataset-id ".xls?meta=" :meta-id)]
          (download-xls-report dataset-id
                               :meta-id
                               :filename)
          => :byte-array
          (provided
           (make-url "forms"  form-xls-url) => :url
           (parse-http :get
                       :url
                       :http-options {:as :byte-array}
                       :as-map? true
                       :filename :filename) => :byte-array)))

  (fact "Should download xls report"
        (let [form-xls-url (str dataset-id ".xls?meta=" :meta-id)]
          (download-xls-report dataset-id
                               :meta-id
                               :filename) => :byte-array
          (provided
           (make-url "forms"  form-xls-url) => :url
           (parse-http :get
                       :url
                       :http-options {:as :byte-array}
                       :as-map? true
                       :filename :filename) => :byte-array)))

  (fact "Should download xls report for single submission"
        (let [form-xls-url (str dataset-id
                                ".xls?meta=" :meta-id "&data_id=" :data-id)]
          (download-xls-report dataset-id
                               :meta-id
                               :filename
                               :data-id)
          => :byte-array
          (provided
           (make-url "forms"  form-xls-url) => :url
           (parse-http :get
                       :url
                       :http-options {:as :byte-array}
                       :as-map? true
                       :filename :filename) => :byte-array)))

  (facts "about clone"
         (fact "Should clone a dataset"
               (clone dataset-id "username") => :response
               (provided
                (make-url "forms" dataset-id "clone.json") => :url
                (parse-http :post
                            :url
                            :http-options {:form-params {:username "username"}}
                            :suppress-4xx-exceptions? true) => :response))

         (fact "Should clone a dataset to project id"
               (clone dataset-id
                      "username" :project-id :project-id) => :response
               (provided
                (make-url "forms" dataset-id "clone.json") => :url
                (parse-http :post
                            :url
                            :http-options {:form-params
                                           {:username "username"
                                            :project_id :project-id}}
                            :suppress-4xx-exceptions? true) => :response)))

  (fact "should generating edit link"
        (let [username "jane"]
          (edit-link username :project-id dataset-id :instance-id) => :response
          (provided
           (make-client-url
            username
            :project-id
            dataset-id
            "submission-editing-complete") => :zebra-url
           (make-url "data"
                     dataset-id
                     :instance-id
                     "enketo.json?return_url=:zebra-url") => :url
           (parse-http :get :url) => {:url :response})))

  (fact "about edit-history"
        (edit-history dataset-id :instance-id) => :response
        (provided
         (make-url "data" dataset-id :instance-id "history.json") => :url
         (parse-http :get :url) => :response)))

(facts "about listing submission files"
       (fact "if dataview null send dataset ID"
             (files :instance-id :project-id
                    :no-cache? true :dataset-id dataset-id)
             => :response
             (provided
              (make-url "metadata.json") => :url
              (parse-http :get :url :no-cache? true
                          :http-options {:query-params {:instance :instance-id
                                                        :project :project-id
                                                        :xform 123}
                                         :content-type :json})
              => :response)))

(fact "about upload project files"
      (upload-file :submission-id {:filename "image.png"}) => :response
      (provided
       (f/uploaded->file {:filename "image.png"}) => :file
       (make-url "metadata.json") => :url
       (parse-http :post
                   :url
                   :http-options {:multipart [{:name "data_value"
                                               :content "image.png"}
                                              {:name "data_type"
                                               :content "supporting_doc"}
                                              {:name "instance"
                                               :content :submission-id}
                                              {:name "data_file"
                                               :content :file}]}
                   :suppress-4xx-exceptions? true)
       => :response))
(fact "about create xform meta permissions"
      (create-xform-meta-permissions 1 "editor-minor" "dataentry-only")
      => :response
      (provided (parse-http
                 :post (make-url "metadata.json")
                 :http-options
                 {:form-params {:data_type  "xform_meta_perms"
                                :xform      1
                                :data_value "editor-minor|dataentry-only"}})
                => :response))
(fact "about update xform meta permissions"
      (update-xform-meta-permissions 1 10 "editor-minor" "dataentry-only")
      => :response
      (provided (parse-http
                 :put (make-url "metadata" "10.json")
                 :http-options
                 {:form-params {:data_type  "xform_meta_perms"
                                :xform      1
                                :data_value "editor-minor|dataentry-only"}})
                => :response))

(facts "about submission reviews"
       (let [status 1
             instance 1
             note "note"
             instances [1 2]
             json-vec (mapv (fn [instance]
                              {:note note :status status :instance instance})
                            instances)
             submission-review-id 1]
         (fact "create submission review"
               (create-submission-review
                {:status status :instance instance :note note})
               => :response
               (provided
                (parse-http
                 :post (make-url "submissionreview.json")
                 :http-options
                 {:form-params
                  {:status status
                   :instance instance
                   :note note}}) => :response))
         (fact "create multiple submission review"
               (create-multiple-submission-reviews
                {:status status :instances instances :note note}) =>
               :response
               (provided
                (parse-http
                 :post (make-url "submissionreview.json")
                 :http-options {:body (generate-string json-vec)
                                :content-type :json}) => :response))
         (fact "get submission review"
               (get-submission-review submission-review-id) => :response
               (provided
                (parse-http
                 :get (make-url "submissionreview" (str submission-review-id
                                                        ".json")))
                => :response))
         (fact "list submission reviews"
               (list-submission-reviews) => :response
               (provided
                (parse-http
                 :get (make-url "submissionreview.json")) => :response))
         (fact "update submission review"
               (update-submission-review
                {:submission-review-id submission-review-id
                 :status status
                 :note note}) => :response
               (provided
                (parse-http
                 :patch (make-url "submissionreview" (str submission-review-id
                                                          ".json"))
                 :http-options
                 {:form-params
                  {:status status :note note}}) => :response))
         (fact "delete submission review"
               (delete-submission-review instance) => :response
               (provided
                (parse-http
                 :delete (make-url "submissionreview" (str submission-review-id
                                                           ".json")))
                => :response))))
