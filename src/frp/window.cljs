(ns frp.window
  (:refer-clojure :exclude [drop])
  (:require [cats.core :as m]
            [frp.browser :as browser :include-macros true]
            [frp.primitives.behavior :as behavior :include-macros true]))

(defn get-coordinate
  [event]
  {:page-x (aget event "pageX")
   :page-y (aget event "pageY")})

(browser/defevent dragstart
  get-coordinate)

(browser/defevent drop
  get-coordinate)

(browser/defevent mousemove
  (fn [event*]
    {:movement-x (aget event* "movementX")
     :movement-y (aget event* "movementY")}))

(browser/defevent mouseup
  (constantly {}))

(browser/defevent popstate
  (fn [_]
    {:location {:pathname js/location.pathname}}))

(browser/defevent resize
  (fn [_]
    {:inner-height js/innerHeight}))

(def inner-height
  (behavior/->Behavior ::inner-height))

(behavior/register
  (behavior/redef inner-height
                  (->> resize
                       (m/<$> :inner-height)
                       (behavior/stepper js/innerHeight))))
