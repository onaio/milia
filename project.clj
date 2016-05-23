(defn js-dir
      "Prefix with full JavaScript directory."
      [path]
      (str "resources/public/js/" path))

(defproject onaio/milia "0.3.9"
  :description "The milia.io Clojure Web API"
  :dependencies [;; CORE MILIA REQUIREMENTS
                 [cheshire "5.5.0"]
                 [clj-http "2.0.1" :exclusions [org.clojure/tools.reader]]
                 [environ "1.0.1"]
                 [org.clojure/clojure "1.8.0"]
                 [org.clojure/tools.logging "0.3.1"]
                 ;;cljs
                 [cljs-hash "0.0.2"]
                 [org.clojure/clojurescript "1.7.228"
                  :exclusions [org.clojure/clojure]]
                 [org.clojure/core.async "0.2.374"]
                 [slingshot "0.12.2"]
                 ;; CLIENT REQUIREMENTS
                 [cljs-http "0.1.39"]]
  :license "Apache 2"
  :url "https://github.com/onaio/milia"
  :plugins [[jonase/eastwood "0.2.1"]
            [lein-bikeshed-ona "0.2.1"]
            [lein-cljfmt "0.3.0"]
            [lein-cljsbuild "1.1.1"]
            [lein-environ "1.0.1"]
            [lein-kibit "0.1.2"]
            [lein-midje "3.1.3"]]
  :cljfmt {:file-pattern #"[^\.#]*\.clj[s]?$"}
  :eastwood {:exclude-linters [:constant-test]
             :add-linters [:unused-fn-args :unused-locals :unused-namespaces
                           :unused-private-vars]
             :namespaces [:source-paths]}
  :profiles {:dev {:dependencies [[midje "1.8.2"]]
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
