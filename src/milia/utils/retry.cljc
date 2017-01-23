(ns milia.utils.retry
  #?(:cljs (:require-macros [cljs.core.async.macros :refer [go]]))
  (:require [chimera.seq :refer [in? mapply]]
            #?(:cljs [cljs.core.async :refer [<! timeout]])
            [milia.api.http :refer [parse-http]]))

(def default-max-retries 1)
(def default-retry-for-statuses [502 503 504])
(def initial-polling-interval 1000) ; Retry polling inverval in ms
(def retry-keys [:max-retries :retry-for-statuses])

(defn retry-parse-http
  "Retry if a status code in retry-for-statuses is returned. Pauses for an
   exponentially backedoff initial-polling-interval in ClojureScript. Does not
   pause between retries in Clojure."
  [method url & {:as options
                 :keys [max-retries retry-for-statuses]
                 :or {max-retries default-max-retries
                      retry-for-statuses default-retry-for-statuses}}]
  (#?(:clj identity :cljs go)
   (loop [polling-interval initial-polling-interval
          retry-count 0]
     (let [{:keys [status] :as response}
           (#?(:clj identity :cljs <!)
            (mapply parse-http method url (apply dissoc options retry-keys)))]
       (if (and (in? retry-for-statuses status) (< retry-count max-retries))
         ;; retry
         (do
           #?(:cljs (<! (timeout polling-interval)))
           (recur (* polling-interval 2) (inc retry-count)))
         ;; exit
         response)))))
