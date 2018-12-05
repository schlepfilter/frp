(ns frp.browser
  (:require [frp.primitives.behavior :as behavior]
            [frp.derived :as derived]))

(def redef
  #(behavior/redef % (derived/event)))


