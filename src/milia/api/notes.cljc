(ns milia.api.notes
  (:refer-clojure :exclude [get list])
  (:require [milia.api.http :refer [parse-http]]
            [milia.utils.remote :refer [make-url]]))

(defn list
  "Returns all notes. Returns notes for a specific submission if
  optional instance ID is proviced"
  [& {:keys [instance-id no-cache?]}]
  (parse-http :get (make-url
                    (str "notes"
                         (when instance-id (str "?instance=" instance-id))))
              :no-cache? no-cache?))

(defn create
  "Creates a note for a submission instance given the note and instance-id.
   Takes an options instance-field argument if note added for specific field."
  [note instance-id & [instance-field]]
  (let [url (make-url "notes")
        params {:note note
                :instance instance-id}
        form-params (if instance-field
                      (assoc params :instance_field instance-field)
                      params)]
    (parse-http :post
                url
                :http-options {:form-params  form-params})))

(defn get
  "Returns a note object given a note ID."
  [note-id]
  (parse-http :get (make-url "notes" note-id)))

(defn delete
  "Deletes a note given a note ID."
  [note-id]
  (parse-http :delete (make-url "notes" note-id)))
