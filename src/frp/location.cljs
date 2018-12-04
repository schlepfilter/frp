(ns frp.location
  (:require [cats.core :as m]
            [frp.history :as history]
            [frp.primitives.behavior :as behavior :include-macros true]
            [frp.window :as window]))

(def pathname
  (behavior/->Behavior ::pathname))

(behavior/register
  (behavior/redef pathname
                  (->> (m/<> window/popstate history/pushstate)
                       (m/<$> (comp :pathname
                                    :location))
                       (behavior/stepper js/location.pathname))))
