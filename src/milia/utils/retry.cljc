(ns milia.utils.retry
  #?(:cljs (:require-macros [cljs.core.async.macros :refer [go]]))
  (:require [chimera.seq :refer [in? mapply]]
            [milia.api.http :refer [parse-http]]))

(def default-max-retries 1)
(def default-retry-for-statuses [502 503 504])
(def retry-keys [:max-retries :retry-for-statuses])

(defn retry-parse-http
  [method url & {:as options
                 :keys [max-retries retry-for-statuses]
                 :or {max-retries default-max-retries
                      retry-for-statuses default-retry-for-statuses}}]
  (#?(:cljs go :clj identity)
   (loop [retry-count 0]
     (let [{:keys [status] :as response}
           (#?(:cljs <! :clj identity)
            (mapply parse-http method url (apply dissoc options retry-keys)))]
       (if (and (in? retry-for-statuses status) (< retry-count max-retries))
         ;; retry
         (recur (inc retry-count))
         ;; exit
         response)))))
