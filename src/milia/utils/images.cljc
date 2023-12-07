(ns milia.utils.images
  (:require [chimera.urls :refer [url]]
            [milia.utils.remote :refer [thumbor-server]]))

(defn resize-image
  "Return a URL for this image resized."
  ([image-url edge-px]
   (resize-image image-url edge-px edge-px))
  ([image-url width-px height-px]
   (resize-image image-url width-px height-px thumbor-server))
  ([image-url width-px height-px image-server-url]
   (str image-server-url
        (url  "unsafe"
              (str width-px "x" height-px) "smart" image-url))))
