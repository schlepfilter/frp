(ns repl
  (:require [figwheel-sidecar.repl-api :as repl-api]))

(def build
  ;TODO extract id
  {:id           "test"
   :source-paths ["src" "test"]
   :compiler     {:output-to            "dev-resources/public/js/main.js"
                  :main                 "frp.test.core"
                  :asset-path           "/js/out"
                  :source-map-timestamp true
                  :preloads             ['devtools.preload]
                  :external-config      {:devtools/config {:features-to-install :all}}}
   :figwheel     true})

(repl-api/start-figwheel!
  {:build-ids  ["test"]
   :all-builds [build]})

(repl-api/cljs-repl)
