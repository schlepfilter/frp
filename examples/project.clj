(defproject examples "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url  "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [org.clojure/clojurescript "1.10.439"]
                 [aid "0.1.2"]
                 [bidi "2.1.4"]
                 [cljs-ajax "0.8.0"]
                 [frp "0.1.3"]
                 [reagent "0.8.1"]]
  :main ^:skip-aot examples.core
  :target-path "target/%s"
  :plugins [[lein-ancient "0.6.10"]
            [lein-cljsbuild "1.1.3"]]
  :cljsbuild {:builds {:prod {:source-paths ["src"]
                              :compiler     {:output-to     "dist/js/main.js"
                                             :main          examples.core
                                             :optimizations :advanced}}}}
  :profiles {:dev {:dependencies [[binaryage/devtools "0.9.10"]
                                  [figwheel-sidecar "0.5.17"]
                                  [org.clojure/tools.namespace "0.2.11"]
                                  [spyscope "0.1.6"]]}})
