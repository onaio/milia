(ns milia.utils.images
  (:require [chimera.urls :refer [url]]
            [milia.utils.remote :refer [thumbor-server]]))

(defn resize-image
  "Return a URL for this image resized."
  ([image-url edge-px]
   (resize-image image-url edge-px edge-px))
  ([image-url width-px height-px]
   (str thumbor-server
        (url  "unsafe"
              (str width-px "x" height-px) "smart" image-url))))
