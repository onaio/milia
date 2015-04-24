(ns ona.api.j2x
  (:require [ona.api.http :refer [parse-http]]
            [ona.utils.file :as file-utils]
            [ona.utils.remote :refer [make-j2x-url]]
            [ona.utils.seq :refer [has-keys?]]))

(defn upload-xls-template
  "Upload xls template to j2x"
  [media-file]
  (let [url (make-j2x-url "templates")
        data-file (file-utils/uploaded->file media-file)
        data-file-bytes (file-utils/to-byte-array data-file)]
    (parse-http :post url nil {:body data-file-bytes
                               :raw-response? true
                               :as-map? true})))

(defn download-xls-report-template
  "Download xls report template from the j2x service"
  [account filename template-token]
  (let [url (make-j2x-url "templates" template-token)]
    (parse-http :get url account {:as :byte-array :as-map? true} filename)))
