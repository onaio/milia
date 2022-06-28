(ns milia.utils.file
  (:require [clojure.java.io :as io])
  (:import [org.apache.commons.io IOUtils]))

(defn to-byte-array
  [data-file]
  (IOUtils/toByteArray (io/input-stream data-file)))

(defn uploaded->file
  "Copy a tempfile into an actual file in a tempdir."
  [{:keys [tempfile filename]}]
  (let [tempdir (System/getProperty "java.io.tmpdir")
        path (str tempdir "/" filename)
        file (clojure.java.io/file path)]
    (.deleteOnExit file)
    (clojure.java.io/copy tempfile file)
    file))
