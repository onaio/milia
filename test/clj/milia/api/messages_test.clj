(ns milia.api.messages_test
  (:require [midje.sweet :refer :all]
            [milia.api.messages :refer :all]
            [milia.api.http :refer [parse-http]]
            [milia.utils.remote :refer [make-url]]))

(def message "This is a message.")
(def form-id :form-id)
(def target-type "xform")
(def params {:target_type target-type
             :target_id form-id
             :message message})
(def post-message-url (make-url "messaging"))
(def get-messages-url
  (make-url (str "messaging?target_type=" target-type "&target_id=" form-id)))

(facts "about messages/get-all-messages"
       (fact "messages/get-all-messages returns response"
             (get-all-messages form-id) => :response
             (provided
              (parse-http :get get-messages-url) => :response)))

(facts "about messages/create-messages"
       (fact "messages/create-message returns response"
             (create-message params) => :response
             (provided
              (parse-http :post post-message-url
                          :http-options {:form-params params})
              => :response)))
