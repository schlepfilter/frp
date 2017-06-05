(ns examples.core
  (:require [bidi.bidi :as bidi]
            [com.rpl.specter :as s]
            [help.core :as help]
            [reagent.core :as r]
            [examples.index :as index]
            [frp.core :as frp]
            [frp.location :as location]))

(def app
  (help/=<< (comp (s/setval :index index/index index/route-function)
                  :handler
                  (partial bidi/match-route index/route))
            location/pathname))

(frp/on (partial (help/flip r/render) (js/document.getElementById "app"))
        app)

(frp/activate)
