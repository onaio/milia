(ns milia.api.rest-services
  (:require [milia.api.http :refer [parse-http]]
            [milia.utils.remote :refer [make-url]]))

(defn create
  "Create a rest service.
   `xform-id` is the numeric id of the x-form to associate the service with
   `name` is one of 1. `generic_json`
                    2. `generic_xml`
                    3. `textit`
                    4. `f2dhis2`
                    5. `bamboo`
   `service-url` is the URL of the webhook receiving endpoint
   `options` is a map for additional configuration, containing the following keys in the case of textit:
    1. `:service` - Service being configured
    2. `:auth_token` - Authentication token
    3. `:flow_uuid` - UUID of the flow in textit
    4. `:contacts` - The contact in the flow"
  [xform-id name service-url & [options]]
  (parse-http :post
              (make-url "restservices")
              :http-options
              {:form-params
               (merge
                {:xform xform-id
                 :name name
                 :service_url service-url}
                options)
               :content-type :json}))

(defn delete
  "Delete a rest service"
  [id]
  (parse-http :delete (make-url "restservices" id)))

(defn get-all
  "Get all rest services the requesting user has access to"
  []
  (parse-http :get (make-url "restservices")))

(defn get-by-id
  "Get information for a specific rest service"
  [id]
  (parse-http :get (make-url "restservices" id)))
