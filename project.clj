(defn js-dir
  "Prefix with full JavaScript directory."
  [path]
  (str "resources/public/js/" path))

(def project-env
  {:debug-api "false"
   :milia-http-default-per-route "10"
   :milia-http-threads "20"})

(defproject onaio/milia "0.7.6"
  :description "The ona.io Clojure Web API Client."
  :dependencies [;; CORE MILIA REQUIREMENTS
                 [cheshire "5.11.0"]
                 [clj-http "3.12.3" :exclusions [com.cognitect/transit-cljs]]
                 [environ "1.2.0"]
                 [onaio/chimera "0.1.2" :exclusions [log4j]]
                 [org.clojure/clojure "1.11.1"]
                 [org.clojure/tools.logging "1.2.4"]
                 [com.fzakaria/slf4j-timbre "0.3.21"]
                 ;;cljs
                 [cljs-hash "0.0.2"]
                 [org.clojure/clojurescript "1.11.60"]
                 [org.clojure/core.async "1.5.648" :exclusions [org.clojure/tools.reader]]
                 [slingshot "0.12.2"]
                 ;; CLIENT REQUIREMENTS
                 [cljs-http "0.1.46" :exclusions [com.cognitect/transit-cljs]]]
  :license "Apache 2"
  :url "https://github.com/onaio/milia"
  :plugins [[jonase/eastwood "1.1.1"]
            [lein-bikeshed "0.5.2"]
            [lein-cljfmt "0.8.0"]
            [lein-cljsbuild "1.1.8"]
            [lein-environ "1.0.1"]
            [lein-kibit "0.1.8"]
            [lein-midje "3.2.2"]
            [lein-doo "0.1.11"]
            [lein-ancient "1.0.0-RC3"]]
  :bikeshed {:var-redefs false
             :name-collisions false}
  :cljfmt {:file-pattern #"[^\.#]*\.clj[s]?$"}
  :eastwood {:exclude-linters [:constant-test :unused-locals :unused-fn-args :unused-private-vars]
             :add-linters [:unused-fn-args :unused-namespaces]
             :namespaces [:source-paths]}
  :profiles {:dev {:dependencies [[midje "1.10.5" :exclusions [joda-time org.clojure/tools.namespace clj-time]]]
                   :env ~project-env}
             :uberjar {:env ~project-env}}
  :test-paths ["test/clj" "target/generated/test/clj"]
  :cljsbuild {:builds {:dev
                       {:compiler {:output-to ~(js-dir "lib/main.js")
                                   :output-dir ~(js-dir "lib/out")
                                   :optimizations :whitespace
                                   :pretty-print true
                                   :source-map ~(js-dir "lib/main.js.map")}}
                       :test
                       {:source-paths ["src" "test"]
                        :compiler {:output-to "test-output/test-file.js"
                                   :main test-runner
                                   :optimizations :none}}
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
                               "target/main-test.js"]}}
  :global-vars {*warn-on-reflection* true})
