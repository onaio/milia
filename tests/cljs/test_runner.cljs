(ns test-runner
  (:require
   [cljs.test :as test :refer-macros [run-tests] :refer [report]]
   [milia.api.async-export-test]
   [milia.api.io-test]
   [milia.api.project-test]))


(enable-console-print!)

(defmethod report [::test/default :summary] [m]
  (println "\nRan" (:test m) "tests containing"
           (+ (:pass m) (:fail m) (:error m)) "assertions.")
  (println (:fail m) "failures," (:error m) "errors.")
  (aset js/window "test-failures" (+ (:fail m) (:error m))))

(defn runner []
  (if (cljs.test/successful?
       (run-tests
        (test/empty-env ::test/default)
        'milia.api.async-export-test
        'milia.api.io-test
        'milia.api.project-test))
    0
    1))
