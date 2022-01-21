(ns test-runner
  (:require
   [doo.runner :refer-macros [doo-tests]]
   [milia.api.async-export-test]
   [milia.api.io-test]
   [milia.api.submissions-test]
   [milia.api.project-test]))

(enable-console-print!)

(doo-tests 'milia.api.async-export-test
           'milia.api.io-test
           'milia.api.submissions-test
           'milia.api.project-test)
