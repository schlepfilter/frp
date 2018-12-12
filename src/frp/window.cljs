(ns frp.window
  (:refer-clojure :exclude [drop])
  (:require [cats.core :as m]
            [cuerdas.core :as cuerdas]
            [frp.browser :as browser :include-macros true]
            [frp.primitives.behavior :as behavior :include-macros true]))

(def get-coordinate
  #(->> #{:page-x :page-y :movement-x :movement-y}
        (mapcat (juxt identity
                      ;TODO don't use aget
                      (comp (partial aget %)
                            cuerdas/camel)))
        (apply array-map)))

(browser/defevent dragstart
  get-coordinate)

(browser/defevent drop
  get-coordinate)

(browser/defevent pointermove
  get-coordinate)

(browser/defevent pointerup
  get-coordinate)

(browser/defevent popstate
  (fn [_]
    {:pathname js/location.pathname}))

(browser/defevent resize
  (fn [_]
    {:inner-height js/innerHeight}))

(browser/defbehavior inner-height
  #(->> resize
        (m/<$> :inner-height)
        (behavior/stepper js/innerHeight)))
