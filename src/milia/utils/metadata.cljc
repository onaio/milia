(ns milia.utils.metadata
  (:require [milia.api.http :refer [parse-http]]
            #?(:clj [milia.utils.file :refer [uploaded->file]])
            [milia.utils.remote :refer [make-url]]))

#?(:clj
   (defn upload-metadata-file
     "Upload `file` to metadata with name `id-name` for content `id`."
     [id-name id file]
     (let [url (make-url "metadata")
           data-file (uploaded->file file)
           muiltipart [{:name "data_value"
                        :content (:filename file)}
                       {:name "data_type"
                        :content "supporting_doc"}
                       {:name id-name
                        :content id}
                       {:name "data_file"
                        :content data-file}]]
       (parse-http :post url
                   :http-options {:multipart muiltipart}
                   :suppress-4xx-exceptions? true))))

(defn metadata-files
  "Fetch `metadata` with query key `id-key` and value `id`."
  [id-key id no-cache? & {:keys [extra-params]}]
  (parse-http :get (make-url "metadata")
              :no-cache? no-cache?
              :http-options {:query-params (conj {id-key id} extra-params)
                             :content-type :json}))
