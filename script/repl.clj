(ns repl
  (:require [com.rpl.specter :as s]
            [figwheel-sidecar.repl-api :as repl-api]
            [taoensso.encore :as encore]))

(def build
  {:id           "test"
   :source-paths ["src" "test"]
   :compiler     {:output-to            "dev-resources/public/js/main.js"
                  :output-dir           "dev-resources/public/js/out"
                  :main                 "frp.test.core"
                  :asset-path           "/js/out"
                  :source-map-timestamp true
                  :preloads             ['devtools.preload]
                  :external-config      {:devtools/config {:features-to-install :all}}}
   :figwheel     true})

(repl-api/start-figwheel!
  {:build-ids  ["test"]
   :all-builds [build]})

(repl-api/cljs-repl "test")
