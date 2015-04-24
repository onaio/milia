(ns ona.api.project
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.core.async :as async :refer [<!]]
            [cljs-http.client :as http]
            [ona.api.io :as io]))

(defn update-project
  "Update the project"
  [projectid owner params]
  (let [url (str "/" owner "/" projectid "/project-settings")
        query-params (merge {:project-id projectid
                             :patch true}
                            params)]
    (io/query-helper! :post url nil query-params)))

(defn update-public
  "Update the project public setting."
  [projectid owner public]
  (update-project projectid owner {:public public}))
