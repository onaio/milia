(ns ona.api.images
  (:require [ona.api.http :refer [parse-http]]
            [ona.api.io :refer [multipart-options]]
            [ona.utils.remote :refer [thumbor-server]]))

(def upload-url (str thumbor-server "/image"))

(defn upload
  "Process an avatar upload. Expects a binary avatar data object."
  [file]
  (when (and file (:size file) (-> file :size zero? not))
    (let [{:keys [headers status]}
          (parse-http :post
                      upload-url
                      nil
                      (merge
                       {:headers {"Slug" (:filename file)}
                        :as-map? true
                        :suppress-40x-exceptions? true}
                       (multipart-options file "media"))
                      nil)]
      (if (> 400 status)
        ;; remove the leading forward-slash
        (str thumbor-server "/" (subs (headers "Location") 1))
        status))))
