(ns milia.api.io-test
  (:require-macros [cljs.test :refer (is deftest testing)])
  (:require [cljs.test :as t]
            [milia.api.io :as io]
            [milia.utils.remote :refer [*credentials*]]))

(deftest build-request-headers
  (let [temp-token "z temp token"]
    (binding [*credentials* (atom {:temp-token temp-token})]
      (is (= (io/token->headers :get-crsftoken? true
                                :must-revalidate? true)
             {"Authorization" (str "TempToken "  temp-token)
              "Cache-control" "must-revalidate"}))
      (is (= (io/token->headers :token temp-token)
             {"Authorization" (str "TempToken "  temp-token)})))))
