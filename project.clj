(defproject frp "0.1.2"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url  "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/clojurescript "1.9.494"]
                 [aid "0.1.1"]
                 [aysylu/loom "1.0.0"]
                 [clj-time "0.13.0"]
                 [com.andrewmcveigh/cljs-time "0.4.0"]
                 [com.rpl/specter "1.0.0"]
                 [funcool/cats "2.0.0"]
                 [frankiesardo/linked "1.2.9"]
                 [jarohen/chime "0.2.1"]]
  :plugins [[com.jakemccrary/lein-test-refresh "0.19.0"]
            [lein-ancient "0.6.10"]]
  :profiles {:dev {:dependencies [[binaryage/devtools "0.9.2"]
                                  [com.taoensso/encore "2.90.1"]
                                  [figwheel-sidecar "0.5.9"]
                                  [org.clojure/test.check "0.9.0"]
                                  [org.clojure/tools.namespace "0.2.11"]
                                  [spyscope "0.1.6"]]}})
