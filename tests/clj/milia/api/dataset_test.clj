(ns milia.api.dataset_test
  (:require [midje.sweet :refer :all]
            [milia.api.dataset :refer :all]
            [milia.utils.file :as f]
            [milia.utils.remote :refer [make-j2x-url make-zebra-url]]
            [milia.api.http :refer [parse-http]]
            [milia.api.io :refer [make-url multipart-options]]))

(let [url :fake-url
      username :fake-username
      password :fake-password
      account {:username username :password password}
      params {:created_by :created_by
              :description :description
              :downloadable :downloadable
              :owner :owner
              :project :project
              :public :public
              :public_data :public_data
              :title :title
              :uuid :uuid
              :version :version}
      suppress-40x {:suppress-40x-exceptions? true}]
  (facts "about datasets"
         (fact "Should get correct url"
               (all account) => :something
               (provided
                (make-url (str "forms?owner=" username)) => url
                (parse-http :get url account) => :something)))

  (facts "about datasets-update"
         (fact "Should get correct url"
               (update account :dataset-id params) => :something
               (provided
                (make-url "forms" :dataset-id) => url
                (parse-http :put url account {:form-params params}) => :something)))

  (facts "about dataset metadata"
         (fact "should get dataset metadata"
               (metadata account :dataset-id) => :fake-metadata
               (provided
                (make-url "forms" ":dataset-id.json") => url
                (parse-http :get url account) => :fake-metadata)))

  (fact "about dataset-getdata"
        (data account :dataset-id) => :something
        (provided
         (make-url "data" :dataset-id) => url
         (parse-http :get url account {:raw-response? nil
                                       :must-revalidate? nil}) => :something))

  (fact "about dataset-getdata :raw"
        (data account :dataset-id :raw? true :must-revalidate? true) => :something
        (provided
         (make-url "data" :dataset-id) => url
         (parse-http :get url account {:raw-response? true
                                       :must-revalidate? true}) => :something))

  (fact "about dataset-getrecord"
        (record account :dataset-id :record-id) => :something
        (provided
         (make-url "data" :dataset-id :record-id) => url
         (parse-http :get url account) => :something))

  (fact "about dataset-get-tags"
        (tags account :dataset-id) => :something
        (provided
         (make-url "forms" :dataset-id "labels") => url
         (parse-http :get url account) => :something))

  (fact "about dataset-add-tag"
        (add-tags  account :dataset-id :tags) => :something
        (provided
         (make-url "forms" :dataset-id "labels") => url
         (parse-http :post url account {:form-params :tags}) => :something))

  (facts "About dataset download"
         (fact "Should make data URL and parse response"
               (let [format "csv"
                     filename (str :dataset-id "." format)]
                 (download account :dataset-id format) => :fake-file
                 (provided
                  (make-url "data" filename) => url
                  (parse-http :get url account {} filename) => :fake-file)))

         (fact "Should change URL for async"
               (let [format "csv"
                     filename (str :dataset-id "." format)]
                 (download account :dataset-id format true) => :fake-file
                 (provided
                  (make-url "forms" filename) => url
                  (parse-http :get url account {} filename) => :fake-file)))

         (fact "Should handle XLS as byte array"
               (let [format "xls"
                     filename (str :dataset-id "." format)]
                 (download account :dataset-id format) => :fake-file
                 (provided
                  (make-url "data" filename) => url
                  (parse-http :get url account {:as :byte-array} filename)
                  => :fake-file)))

         (fact "Should handle csvzip zip extension"
               (let [format "csvzip"
                     path (str :dataset-id "." format)
                     filename (str :dataset-id ".zip")]
                 (download account :dataset-id format) => :fake-file
                 (provided
                  (make-url "data" path) => url
                  (parse-http :get url account {:as :byte-array} filename)
                  => :fake-file))))

  (facts "about dataset form"
         (fact "Return JSON string"
               (form account :dataset-id) => :json
               (provided
                (make-url "forms" :dataset-id "form.json") => url
                (parse-http :get url account) => :json))

         (fact "Download as format"
               (let [format "csv"
                     suffix (str "form." format)
                     filename (str :dataset-id "_" suffix)]
                 (form account :dataset-id format) => :fake-file
                 (provided
                  (make-url "forms" :dataset-id suffix) => url
                  (parse-http :get url account {} filename) => :fake-file))))

  (fact "about online-data-entry-link"
        (online-data-entry-link account :dataset-id) => :enketo_url
        (provided
         (make-url "forms" :dataset-id "enketo") => url
         (#'milia.api.io/http-request :get url {}) =>
         {:body :body
          :request :request
          :status :status}
         (#'milia.api.io/add-to-options account suppress-40x url) => {}
         (milia.api.io/parse-response :body :status nil nil) => {:enketo_url :enketo_url}))

  (fact "about dataset delete"
        (delete account :dataset-id) => :response
        (provided
         (make-url "forms" :dataset-id "delete_async") => url
         (parse-http :delete url account) => :response))

  (fact "about create dataset"
        (let [options {:multipart [{:name "xls_file"
                                    :content :xlsfile}]}]
          (create account {:xls_file :uploaded-file}) => :response
          (provided
           (multipart-options :uploaded-file "xls_file") => options
           (make-url "forms") => url
           (parse-http :post
                       url
                       account
                       options) => :response)))

  (fact "about move dataset to folder"
        (move-to-project account 1 :project-id) => :form
        (provided
         (make-url "projects" :project-id "forms") => url
         (parse-http :post
                     url
                     account
                     {:form-params {:formid 1}}) => :form))

  (facts "about update-sharing for dataset"
         (let [username :fake-username
               role :fake-role
               data {:username username :role role}]
           (fact "Should return result of parse-http"
                 (update-sharing account
                                 :dataset-id
                                 username
                                 role) => :sharing-updated
                 (provided
                  (make-url "forms" :dataset-id "share") => url
                  (parse-http :post url account {:form-params data})
                  => :sharing-updated))))

  (fact "about upload media"
        (upload-media account :dataset-id {:filename "image.png"}) => :response
        (provided
         (f/uploaded->file {:filename "image.png"}) => :media-file
         (make-url "metadata") => url
         (parse-http :post
                     url
                     account
                     {:multipart [{:name "data_value"
                                   :content "image.png"}
                                  {:name "data_type"
                                   :content "media"}
                                  {:name "xform"
                                   :content :dataset-id}
                                  {:name "data_file"
                                   :content :media-file}]}) => :response))

  (facts "about xls template reports"
         (let [media-file {:filename "filename"}
               uuid "12345621"
               add-xls-response {:status 200
                                 :body :xls-metadata}
               byte-array [0,1]
               data-value (str ":filename|" (make-j2x-url "xls" ":uuid"))]
           (fact "Should add xls report to Ona"
                 (add-xls-report account
                                 :dataset-id
                                 :uuid
                                 :filename) => (contains add-xls-response)
                 (provided
                  (make-url "metadata") => url
                  (parse-http :post
                              url
                              account
                              {:form-params
                               {:xform :dataset-id
                                :data_type "external_export"
                                :data_value data-value}})
                  => add-xls-response)))

         (facts "About patch"
                (let [options {:form-params :params}
                      file {:xls_file :uploaded-file}
                      multipart-options-map {:multi :part}]
                  (fact "Should call parse-http with patch"
                        (patch account :dataset-id :params) => :response
                        (provided
                         (make-url "forms" :dataset-id) => url
                         (parse-http :patch
                                     url
                                     account
                                     options) => :response))

                  (fact "Should call parse-http with multipart options"
                        (patch account :dataset-id :params file) => :response
                        (provided
                         (make-url "forms" :dataset-id) => url
                         (multipart-options :uploaded-file "xls_file")
                         => multipart-options-map
                         (parse-http :patch
                                     url
                                     account
                                     multipart-options-map)
                         => :response))))

         (facts "About CSV Imports"
                (fact "should import csv file to dataset endpoint"
                      (let [multipart-options-map {:multi :part}]
                        (csv-import account :dataset-id :file) => :response
                        (provided
                         (make-url "forms" :dataset-id "csv_import") => url
                         (multipart-options :file "csv_file")
                         => multipart-options-map
                         (parse-http :post :fake-url account multipart-options-map)
                         => :response)))))

  (fact "Should download xls report"
        (let [form-xls-url (str :dataset-id ".xls?meta=" :meta-id)]
          (download-xls-report :account
                               :dataset-id
                               :meta-id
                               :filename) => :byte-array
          (provided
           (make-url "forms"  form-xls-url) => :url
           (parse-http :get
                       :url
                       :account
                       {:as :byte-array :as-map? true}
                       :filename) => :byte-array))))

(fact "Should download xls report"
      (let [form-xls-url (str :dataset-id ".xls?meta=" :meta-id)]
        (download-xls-report :account
                             :dataset-id
                             :meta-id
                             :filename) => :byte-array
        (provided
          (make-url "forms"  form-xls-url) => :url
          (parse-http :get
                      :url
                      :account
                      {:as :byte-array :as-map? true}
                      :filename) => :byte-array)))

(fact "Should download xls report for single submission"
      (let [form-xls-url (str :dataset-id ".xls?meta=" :meta-id"&data_id=":data-id)]
        (download-xls-report :account
                             :dataset-id
                             :meta-id
                             :filename
                             :data-id) => :byte-array
        (provided
          (make-url "forms"  form-xls-url) => :url
          (parse-http :get
                      :url
                      :account
                      {:as :byte-array :as-map? true}
                      :filename) => :byte-array)))

(fact "Should clone a dataset"
      (clone :account :dataset-id :username) => :response
      (provided
       (make-url "forms" :dataset-id "clone") => :url
       (parse-http :post :url :account {:form-params {:username :username}
                                        :suppress-40x-exceptions? true}) => :response))

(fact "about generating edit link"
      (let [username "bob"
           account {:username username}]
        (edit-link account :project-id :dataset-id :instance-id) => :response
        (provided
          (make-zebra-url username :project-id :dataset-id "submission-editing-complete") => :zebra-url
          (make-url "data" :dataset-id :instance-id "enketo?return_url=:zebra-url") => :url
          (parse-http :get :url account) => {:url :response})))