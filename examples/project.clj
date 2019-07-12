(defproject examples "0.1.0-SNAPSHOT"
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [org.clojure/clojurescript "1.10.439"]
                 [aid "0.1.2"]
                 [bidi "2.1.4"]
                 [cljsjs/mousetrap "1.5.3-0"]
                 [frp "0.1.3"]
                 [reagent "0.8.1"]
                 [thi.ng/color "1.3.0"]]
  :plugins [[lein-ancient "0.6.15"]
            [lein-cljsbuild "1.1.7"]]
  :profiles {:dev {:dependencies [[binaryage/devtools "0.9.10"]
                                  [figwheel-sidecar "0.5.17"]
                                  [org.clojure/tools.namespace "0.2.11"]
                                  [spyscope "0.1.6"]]}}
  :cljsbuild {:builds
              {:prod
               {:source-paths ["src"]
                :compiler     {:output-to       "resources/public/js/main.js"
                               :main            examples.core
                               :optimizations   :advanced
                               :closure-defines {goog.DEBUG false}}}}})
