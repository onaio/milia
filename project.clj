(defproject onaio/milia "0.1.0-SNAPSHOT"
  :description "The ona.io Clojure Web API"
  :dependencies [;; CORE MILIA REQUIREMENTS
                 [org.clojure/clojure "1.6.0"]
                 [org.clojure/clojurescript "0.0-2843"]
                 [org.clojure/core.async "0.1.346.0-17112a-alpha"]
                 [cheshire "5.2.0"]
                 [clj-http "1.0.1"]
                 [sablono "0.3.1"]
                 [prismatic/dommy "0.1.2"]
                 [org.omcljs/om "0.8.8"]
                 [inflections "0.9.7"]
                 ;; ring middleware
                 [slingshot "0.12.2"]
                 ;; CLJX
                 [com.keminglabs/cljx "0.6.0" :exclusions [org.clojure/clojure]]
                 ;; CLIENT REQUIREMENTS
                 [cljs-http "0.1.17"]
                 [com.cognitect/transit-cljs "0.8.188"]]

  :plugins [[lein-cljsbuild "1.0.5"]
            [lein-midje "3.1.3"]
            [com.keminglabs/cljx "0.6.0" :exclusions [org.clojure/clojure]]]
  :prep-tasks [["cljx" "once"] "javac" "compile"]
  :source-paths ["src/clj"
                 "src/cljs"
                 "target/generated/src/clj"
                 "target/generated/src/cljs"
                 "target/classes"]
  :test-paths ["test/clj" "target/generated/test/clj"]

  :clean-targets ["out/ona" "out/milia" "out/milia.js"]

  :cljx {:builds [{:source-paths ["src/cljx"]
                   :output-path "target/generated/src/clj"
                   :rules :clj}
                  {:source-paths ["src/cljx"]
                   :output-path "target/generated/src/cljs"
                   :rules :cljs}]}
  :cljsbuild {
    :builds [{:id "milia"
              :source-paths ["src/cljs"
                             "target/generated/src/cljs"]
              :compiler {
                :output-to "out/milia.js"
                :output-dir "out"
                :optimizations :none
                :cache-analysis true
                :source-map true}}]})
