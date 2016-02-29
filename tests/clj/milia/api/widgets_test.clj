(ns milia.api.widgets-test
  (:refer-clojure :exclude [get list update])
  (:require [midje.sweet :refer :all]
            [milia.api.http :refer [parse-http]]
            [milia.api.widgets :refer :all]
            [milia.utils.remote :refer [hosts make-url]]))

(def widgets-url "https://stage.ona.io/api/v1/widgets")

(let [widget-definition {:title "A Widgy Widgy Woo"
                         :description "The Widget to end all Widgets"
                         :content_type :form
                         :content_id 12345
                         :widget_type "charts"
                         :view_type "horizontal-bar-chart"}
      content-object-url "https://stage.ona.io/api/v1/forms/12345"
      widgets-url-with-data (str widgets-url "?data=true")]

  (fact "widgets/generate-content-object-url"
        (let [{:keys [content_type content_id]} widget-definition]
          (generate-content-object-url content_type content_id)
          => content-object-url))

  (fact "widgets/create returns the API response"
        (create widget-definition) => :some-widget
        (provided
         (parse-http :post
                     widgets-url
                     :http-options {:json-params (assoc widget-definition
                                                        :content_object
                                                        content-object-url)})
         => :some-widget))

  (fact "widgets/create with-data returns the API response with data"
        (create widget-definition :with-data? true) => :some-widget
        (provided
         (parse-http :post
                     widgets-url-with-data
                     :http-options {:json-params (assoc widget-definition
                                                        :content_object
                                                        content-object-url)})
         => :some-widget)))

(def dataview-id 1)
(def dataview-filter-url (str widgets-url "?dataviewid=" dataview-id))
(def xform-id 1)
(def xform-filter-url (str widgets-url "?xform=" xform-id))
(def widget-id 1)
(def single-widget-url (make-url "widgets" widget-id))

(facts "about widgets/list"
       (fact "widgets/list returns the API response without filters"
             (list) => :response
             (provided
              (parse-http :get widgets-url :http-options {:content-type :json})
              => :response))
       (fact "widgets/list returns API response when filtered by dataview ID"
             (list :dataview-id dataview-id) => :response
             (provided
              (parse-http :get dataview-filter-url
                          :http-options {:content-type :json})
              => :response))
       (fact "widgets/list returns API response when filtered by xform ID"
             (list :xform-id xform-id) => :response
             (provided
              (parse-http :get xform-filter-url
                          :http-options {:content-type :json})
              => :response)))

(facts "about widgets/update"
       (let [patch-map {:order 2
                        :aggregation "mode"}]
         (fact "widgets/update returns the API response"
               (update widget-id patch-map) => :response
               (provided
                (parse-http :patch single-widget-url
                            :http-options {:json-params patch-map})
                => :response))))

(facts "about widget/delete"
       (fact "widget/delete returns the API response"
             (delete widget-id) => :response
             (provided
              (parse-http :delete single-widget-url) => :response)))

(facts "about widget/get"
       (fact "widget/get returns the API response"
             (get widget-id) => :response
             (provided
              (parse-http :get single-widget-url) => :response))

       (fact "widget/get returns the API response when :with-data? is true"
             (get widget-id) => :response
             (provided
              (parse-http :get single-widget-url) => :response)))
