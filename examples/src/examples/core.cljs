(ns examples.core
  (:require [aid.core :as aid]
            [bidi.bidi :as bidi]
            [cats.core :as m]
            [com.rpl.specter :as s]
            [reagent.core :as r]
            [examples.index :as index]
            [frp.core :as frp]
            [frp.location :as location]))

(def app
  (m/=<< (comp (s/setval :index index/index index/route-function)
               :handler
               (partial bidi/match-route index/route))
         location/pathname))

(frp/on (partial (aid/flip r/render) (js/document.getElementById "app"))
        app)

(frp/activate)
