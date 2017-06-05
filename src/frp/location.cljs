(ns frp.location
  (:require [help.core :as help]
            [frp.history :as history]
            [frp.primitives.behavior :as behavior :include-macros true]
            [frp.window :as window]))

(def pathname
  (behavior/->Behavior ::pathname))

(behavior/register
  (behavior/redef pathname
                  (->> (help/<> window/popstate history/pushstate)
                       (help/<$> (comp :pathname
                                       :location))
                       (behavior/stepper js/location.pathname))))
