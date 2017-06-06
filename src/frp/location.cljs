(ns frp.location
  (:require [aid.core :as aid]
            [frp.history :as history]
            [frp.primitives.behavior :as behavior :include-macros true]
            [frp.window :as window]))

(def pathname
  (behavior/->Behavior ::pathname))

(behavior/register
  (behavior/redef pathname
                  (->> (aid/<> window/popstate history/pushstate)
                       (aid/<$> (comp :pathname
                                      :location))
                       (behavior/stepper js/location.pathname))))
