(ns frp.window
  (:refer-clojure :exclude [drop])
  (:require [aid.core :as aid]
            [cats.core :as m]
            [cuerdas.core :as cuerdas]
            [frp.browser :as browser :include-macros true]
            [frp.primitives.behavior :as behavior :include-macros true]))

(defn get-coordinate
  [event*]
  (->> #{:page-x :page-y :movement-x :movement-y}
       (mapcat (aid/build vector
                          identity
                          (comp (partial aget event*)
                                cuerdas/camel)))
       (apply array-map)))

(browser/defevent dragstart
  get-coordinate)

(browser/defevent drop
  get-coordinate)

(browser/defevent mousemove
  get-coordinate)

(browser/defevent mouseup
  get-coordinate)

(browser/defevent popstate
  (fn [_]
    {:location {:pathname js/location.pathname}}))

(browser/defevent resize
  (fn [_]
    {:inner-height js/innerHeight}))

(browser/defbehavior inner-height
  #(->> resize
        (m/<$> :inner-height)
        (behavior/stepper js/innerHeight)))
