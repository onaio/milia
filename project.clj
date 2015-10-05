(defn js-dir
      "Prefix with full JavaScript directory."
      [path]
      (str "resources/public/js/" path))

(defproject onaio/milia "0.2.11-SNAPSHOT"
  :description "The milia.io Clojure Web API"
  :dependencies [;; CORE MILIA REQUIREMENTS
                 [cheshire "5.5.0"]
                 [clj-http "2.0.0" :exclusions [org.clojure/tools.reader]]
                 [environ "1.0.0"]
                 [org.clojure/clojure "1.7.0"]
                 [org.clojure/tools.logging "0.3.1"]
                 ;;cljs
                 [cljs-hash "0.0.2"]
                 [org.clojure/clojurescript "1.7.28"
                  :exclusions [org.clojure/clojure]]
                 [org.clojure/core.async "0.1.346.0-17112a-alpha"]
                 [slingshot "0.12.2"]
                 ;; CLIENT REQUIREMENTS
                 [cljs-http "0.1.35"]]
  :license "Apache 2"
  :url "https://github.com/onaio/milia"
  :plugins [[lein-cljsbuild "1.0.5"]
            [lein-midje "3.1.3"]
            [lein-environ "1.0.0"]]
  :profiles {:dev {:dependencies [[midje "1.7.0"]]
                   :env {:debug-api? false
                         :jetty-min-threads 10
                         :jetty-max-threads 80}}
             :uberjar {:env {:debug-api? false
                             :jetty-min-threads 10
                             :jetty-max-threads 80}}}
  :test-paths ["tests/clj" "target/generated/tests/clj"]
  :cljsbuild {
              :builds {:dev
                       {:compiler {:output-to ~(js-dir "lib/main.js")
                                   :output-dir ~(js-dir "lib/out")
                                   :optimizations :whitespace
                                   :pretty-print true
                                   :source-map ~(js-dir "lib/main.js.map")}}
                       :test
                       {:source-paths ["src" "tests/cljs"]
                        :notify-command ["phantomjs"
                                         "phantom/unit-test.js"
                                         "phantom/unit-test.html"
                                         "target/main-test.js"]
                        :compiler {:output-to "target/main-test.js"
                                   :optimizations :whitespace
                                   :pretty-print true}}
                       :prod
                       {:source-paths ["src"]
                        :compiler {:output-to ~(js-dir "lib/milia.js")
                                   :output-dir ~(js-dir "lib/out-prod")
                                   :optimizations :advanced
                                   :pretty-print false}
                        :jar true}}
              :test-commands {"unit-test"
                              ["phantomjs"
                               "phantom/unit-test.js"
                               "phantom/unit-test.html"
                               "target/main-test.js"]}})
