(ns milia.api.widgets
  (:require [milia.api.http :refer [parse-http]]
            [milia.api.io :refer [make-url]]))

(defn generate-content-object-url
  "The Ona API expects clients to submit a URL as the value of the object bound
   to a widget. This is basically asking clients to be aware of internal API
   implementation details since this is a Django REST framework peculiarity.
   This function provides an abstraction that allows Clojure based clients to
   be ignorant of the quirk.
   `content-type` can be either :form or :dataview
   `content-id` is an integer identifying the object"
  [content-type content-id]
  (make-url (case content-type
             :form "forms"
             :dataview "dataviews")
            content-id))

(defn create
  "Create a new widget.
   `account` is a map representing the authenticating user's credentials
   `widget-definition` is a map containing the following keys:
    `:title` is a string
    `:content-type` is one of either :form of :dataview
    `:content-id` is an intger identifying the content object
    `:description` is a string.
    `:widget_type` is an arbitrary string, determined by the client e.g. chart
    `:view_type` is an arbitrary string, determined by the client e.g. bar-chart
    `:column` is the  data column to be stored based on the form field.
    `:group_by` the data column for the data to be grouped by. Optional"
  [account
   {:keys [content-type
           content-id]
    :as widget-definition}]
  (let [url (make-url "widgets")
        processed-widget-definition
        (assoc widget-definition
          :content_object
          (generate-content-object-url
           content-type
           content-id))]
    (parse-http :post
                url
                account
                {:form-params processed-widget-definition
                 :content-type :json})))

(defn list
  [account]
  (parse-http :get
              (make-url "widgets")
              account
              {:content-type :json}))

(defn list-by-form
  [account xform-id]
  (let [url (make-url (str "widgets?xform=" xform-id))]
    (parse-http :get
                url
                account
                {:content-type :json})))
