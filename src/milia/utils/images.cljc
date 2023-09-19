(ns milia.utils.images
  (:require [chimera.urls :refer [url]]
            [clojure.string :refer [starts-with?]]
            [milia.utils.remote :refer [thumbor-server]]))

(defn resize-image
  "Return a URL for this image resized."
  ([image-url edge-px]
   (resize-image image-url edge-px edge-px))
  ([image-url width-px height-px]
   (resize-image image-url width-px height-px thumbor-server))
  ([image-url width-px height-px image-server-url]
   (let [thumbor-server-prefix (str image-server-url "/image/")]
     (str image-server-url
          (url
           "unsafe"
           (str width-px "x" height-px) "smart"
           (if (starts-with? image-url thumbor-server-prefix)
             (subs image-url (count thumbor-server-prefix))
             image-url))))))
