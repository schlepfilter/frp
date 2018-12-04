(defproject frp "0.1.3"
  ;TODO delete unused keys
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url  "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [org.clojure/clojurescript "1.10.439"]
                 [aid "0.1.2"]
                 [aysylu/loom "1.0.2"]
                 [clj-time "0.15.1"]
                 [com.andrewmcveigh/cljs-time "0.5.2"]
                 [com.rpl/specter "1.1.2"]
                 [funcool/cats "2.3.1"]
                 [frankiesardo/linked "1.3.0"]
                 [jarohen/chime "0.2.2"]]
  :plugins [[com.jakemccrary/lein-test-refresh "0.19.0"]
            [lein-ancient "0.6.10"]]
  :profiles {:dev {:dependencies [[binaryage/devtools "0.9.10"]
                                  [com.taoensso/encore "2.105.0"]
                                  [figwheel-sidecar "0.5.17"]
                                  [org.clojure/test.check "0.9.0"]
                                  [org.clojure/tools.namespace "0.2.11"]
                                  [spyscope "0.1.6"]]}})
