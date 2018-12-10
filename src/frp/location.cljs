(ns frp.location
  (:require [cats.core :as m]
            [frp.browser :as browser :include-macros true]
            [frp.history :as history]
            [frp.primitives.behavior :as behavior :include-macros true]
            [frp.window :as window]))

(browser/defbehavior pathname
  #(->> (m/<> window/popstate history/pushstate)
        (m/<$> (comp :pathname
                     :location))
        (behavior/stepper js/location.pathname)))
