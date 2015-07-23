(ns milia.api.widgets-test
  (:require [midje.sweet :refer :all]
            [milia.api.http :refer [parse-http]]
            [milia.api.widgets :refer :all]
            [milia.utils.remote :refer [hosts make-url]]))

(let [widget-definition {:title "A Widgy Widgy Woo"
                         :description "The Widget to end all Widgets"
                         :content_type :form
                         :content_id 12345
                         :widget_type "charts"
                         :view_type "horizontal-bar-chart"}
      content-object-url "https://stage.ona.io/api/v1/forms/12345"
      widgets-url "https://stage.ona.io/api/v1/widgets"]

  (fact "widgets/generate-content-object-url"
    (let [{:keys [content_type content_id]} widget-definition]
      (generate-content-object-url content_type content_id)
      => content-object-url))

  (fact "widgets/create returns the API response"
    (create widget-definition) => :some-widget
    (provided
     (parse-http :post
                 widgets-url
                 :http-options {:content-type :json
                                :form-params (assoc widget-definition
                                               :content_object
                                               content-object-url)})
     => :some-widget)))
