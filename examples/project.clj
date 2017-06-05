(defproject examples "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url  "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [bidi "2.1.0"]
                 [cljs-ajax "0.6.0"]
                 [frp "0.1.0"]
                 [help "0.1.0"]
                 [reagent "0.6.1"]]
  :main ^:skip-aot examples.core
  :target-path "target/%s"
  :profiles {:dev {:dependencies [[binaryage/devtools "0.9.2"]
                                  [com.taoensso/encore "2.90.1"]
                                  [figwheel-sidecar "0.5.9"]
                                  [org.clojure/tools.namespace "0.2.11"]
                                  [spyscope "0.1.6"]]}})
