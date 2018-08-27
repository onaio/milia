(ns milia.utils.file
  (:require [clojure.java.io :as io])
  (:import [org.apache.commons.io IOUtils]))

(def file-extension-regex #"\w+$")

(defn to-byte-array
  [data-file]
  (IOUtils/toByteArray (io/input-stream data-file)))

(defn uploaded->file
  "Copy a tempfile into an actual file in a tempdir."
  [{:keys [tempfile filename]}]
  (let [tempdir (com.google.common.io.Files/createTempDir)
        path (str (.getAbsolutePath tempdir) "/" filename)
        file (clojure.java.io/file path)]
    (.deleteOnExit file)
    (.deleteOnExit tempdir)
    (clojure.java.io/copy tempfile file)
    file))

(defn get-file-extension
  "Get the file extension given full file name.
   String -> String"
  [filename]
  (re-find file-extension-regex filename))