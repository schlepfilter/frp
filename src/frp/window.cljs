(ns frp.window
  (:require [cats.core :as m]
            [com.rpl.specter :as s]
            [frp.io :as io]
            [frp.primitives.behavior :as behavior :include-macros true]
            [frp.primitives.event :as event]))

(def mousemove
  (event/->Event ::mousemove))

(def mouseup
  (event/->Event ::mouseup))

(def popstate
  (event/->Event ::popstate))

(def resize
  (event/->Event ::resize))

(def inner-height
  (behavior/->Behavior ::inner-height))

(defn add-remove-listener
  [event-type listener]
  (js/addEventListener (name event-type) listener)
  (swap! event/network-state
         (partial s/setval*
                  :cancel
                  (fn [_]
                    (js/removeEventListener event-type listener)))))

(behavior/register
  (io/redef-events mousemove mouseup popstate resize)

  (behavior/redef inner-height
                  (->> resize
                       (m/<$> :inner-height)
                       (behavior/stepper js/innerHeight)))

  ;TODO define a macro to define behaviors and add and remove event listeners
  (add-remove-listener :mousemove
                       (fn [event*]
                         ;(.-movementX event*) is undefined in :advanced.
                         (mousemove {:movement-x (aget event* "movementX")
                                     :movement-y (aget event* "movementY")})))

  (add-remove-listener :mouseup
                       (fn [_]
                         (mouseup {})))

  (add-remove-listener
    :popstate
    (fn [_]
      (popstate {:location {:pathname js/location.pathname}})))

  (add-remove-listener :resize
                       (fn [_]
                         (resize {:inner-height js/innerHeight}))))
