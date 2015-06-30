(defn js-dir
      "Prefix with full JavaScript directory."
      [path]
      (str "resources/public/js/" path))

(defproject onaio/milia "0.1.3"
  :description "The milia.io Clojure Web API"
  :dependencies [;; CORE MILIA REQUIREMENTS
                 [cheshire "5.2.0"]
                 [clj-http "1.0.1" :exclusions [org.clojure/tools.reader]]
                 [environ "1.0.0"]
                 [org.clojure/clojure "1.7.0"]
                 ;;cljs
                 [cljs-hash "0.0.2"]
                 [org.clojure/clojurescript "0.0-3308"
                  :exclusions [org.clojure/clojure]]
                 [org.clojure/core.async "0.1.346.0-17112a-alpha"]
                 [sablono "0.3.1"]
                 [prismatic/dommy "0.1.2"]
                 [org.omcljs/om "0.8.8"]
                 [inflections "0.9.7"]
                 [slingshot "0.12.2"]
                 ;; CLJX
                 [com.keminglabs/cljx "0.6.0"
                  :exclusions [org.clojure/clojure]]
                 ;; CLIENT REQUIREMENTS
                 [cljs-http "0.1.17"]
                 [com.cognitect/transit-cljs "0.8.188"]]
  :license "Apache 2"
  :url "https://github.com/onaio/milia"
  :plugins [[com.keminglabs/cljx "0.6.0" :exclusions [org.clojure/clojure]]
            [lein-cljsbuild "1.0.5"]
            [lein-midje "3.1.3"]
            [lein-environ "1.0.0"]]
  :profiles {:dev {:dependencies [[midje "1.6.3"]]
                   :env {:debug-api? false
                         :jetty-min-threads 10
                         :jetty-max-threads 80}}
             :uberjar {:env {:debug-api? false
                             :jetty-min-threads 10
                             :jetty-max-threads 80}}}
  :prep-tasks [["cljx" "once"] "javac" "compile"]
  :source-paths ["src/clj"
                 "src/cljs"
                 "target/generated/src/clj"
                 "target/generated/src/cljs"]
  :test-paths ["tests/clj" "target/generated/tests/clj"]
  :cljsbuild {
              :builds {:dev
                       {:source-paths ["src/cljs"
                                       "target/generated/src/cljs"]
                        :compiler {:output-to ~(js-dir "lib/main.js")
                                   :output-dir ~(js-dir "lib/out")
                                   :optimizations :whitespace
                                   :pretty-print true
                                   :source-map ~(js-dir "lib/main.js.map")}}
                       :test
                       {:source-paths ["src/cljs"
                                       "tests/cljs"
                                       "target/generated/src/cljs"]
                        :notify-command ["phantomjs"
                                         "phantom/unit-test.js"
                                         "phantom/unit-test.html"
                                         "target/main-test.js"]
                        :compiler {:output-to "target/main-test.js"
                                   :optimizations :whitespace
                                   :pretty-print true}}
                       :prod
                       {:source-paths ["src/cljs"
                                       "target/generated/src/cljs"]
                        :compiler {:output-to ~(js-dir "lib/milia.js")
                                   :output-dir ~(js-dir "lib/out-prod")
                                   :optimizations :advanced
                                   :pretty-print false}
                        :jar true}}
              :test-commands {"unit-test"
                              ["phantomjs"
                               "phantom/unit-test.js"
                               "phantom/unit-test.html"
                               "target/main-test.js"]}}
  :cljx {:builds [{:source-paths ["src/cljx"]
                   :output-path "target/generated/src/clj"
                   :rules :clj}
                  {:source-paths ["src/cljx"]
                   :output-path "target/generated/src/cljs"
                   :rules :cljs}]})
