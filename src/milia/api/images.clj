(ns milia.api.images
  (:require [milia.api.http :refer [parse-http]]
            [milia.api.io :refer [multipart-options]]
            [milia.utils.remote :refer [thumbor-server]]))

(def upload-url (str thumbor-server "/image"))

(defn upload
  "Process an avatar upload. Expects a binary avatar data object."
  [file & [image-server-url]]
  (when (and file (:size file) (-> file :size zero? not))
    (let [{:keys [headers status]}
          (parse-http :post
                      (or (and image-server-url
                               (str image-server-url "/image"))
                          upload-url)
                      :http-options
                      (merge
                       {:headers {"Slug" (:filename file)}}
                       (multipart-options file "media"))
                      :as-map? true
                      :suppress-4xx-exceptions? true)]
      (when (= status 201)
        ;; A 201 HTTP status code indicates success
        (str (or image-server-url thumbor-server)
             "/"
             (subs (headers "Location") 1))))))
