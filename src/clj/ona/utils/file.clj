(ns ona.utils.file
  (:require [clojure.java.io :as io])
  (:import [org.apache.commons.io IOUtils]))

(defn to-byte-array [data-file]
  (IOUtils/toByteArray (io/input-stream data-file)))

(defn uploaded->file [uploaded-file]
  (let [{:keys [tempfile filename]} uploaded-file
        tempdir (com.google.common.io.Files/createTempDir)
        path (str (.getAbsolutePath tempdir) "/" filename)
        file (clojure.java.io/file path)]
    (.deleteOnExit file)
    (.deleteOnExit tempdir)
    (clojure.java.io/copy tempfile file)
    file))
