(ns milia.api.notes-test
  (:refer-clojure :exclude [get list update])
  (:require [midje.sweet :refer :all]
            [milia.api.http :refer [parse-http]]
            [milia.api.notes :refer :all]
            [milia.utils.remote :refer [hosts make-url]]))

(def note "This is a note.")
(def instance-id :instance-id)
(def note-id :note-id)
(def notes-url (make-url "notes"))
(def single-notes-url (make-url "notes" note-id))
(def instance-note {:note note
                    :instance instance-id})
(def instance-field :instance-field)
(def instance-field-note (assoc instance-note :instance_field instance-field))

(facts "about notes/list"
       (fact "notes/list returns response"
             (list) => :response
             (provided
              (parse-http :get notes-url) => :response)))

(facts "about notes/create"
       (fact "notes/create for instance returns response"
             (create note instance-id) => :response
             (provided
              (parse-http :post notes-url
                          :http-options {:json-params instance-note})
              => :response))
       (fact "notes/create for instance field returns response"
             (create note instance-id instance-field) => :response
             (provided
              (parse-http :post notes-url
                          :http-options {:json-params instance-field-note})
              => :response)))

(facts "about notes/get"
       (fact "notes/get returns response"
             (get note-id) => :response
             (provided
              (parse-http :get single-notes-url) => :response)))

(facts "about notes/delete"
       (fact "notes/delete returns response"
             (delete note-id) => :response
             (provided
              (parse-http :delete single-notes-url) => :response)))