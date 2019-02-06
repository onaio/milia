(ns milia.api.messages
  (:require [milia.api.http :refer [parse-http]]
            [milia.utils.remote :refer [make-url]]))

(defn create-message
  "Create message"
  [params]
  (let [url (make-url "messaging.json")]
    (parse-http :post url :http-options {:form-params params})))

(defn get-all-messages
  "List all the messages belonging to a particular formid."
  [form-id & {:keys [target-type] :or {target-type "xform"}}]
  (let [url (make-url
             (str "messaging.json?target_type="
                  target-type
                  "&target_id="
                  form-id))]
    (parse-http :get url)))