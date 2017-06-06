(ns frp.window
  (:require [com.rpl.specter :as s]
            [aid.core :as help]
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
  (js/addEventListener event-type listener)
  (swap! event/network-state
         (partial s/setval*
                  :cancel
                  (fn [_]
                    (js/removeEventListener event-type listener)))))

(behavior/register
  (io/redef-events [popstate resize mousemove mouseup])

  (behavior/redef inner-height
                  (->> resize
                       (help/<$> :inner-height)
                       (behavior/stepper js/innerHeight)))

  ;TODO define a macro to define behaviors and add and remove event listeners
  (add-remove-listener
    "popstate"
    #(popstate {:location {:pathname js/location.pathname}}))

  (add-remove-listener "resize"
                       #(resize {:inner-height js/innerHeight}))

  (add-remove-listener "mousemove"
                       (fn [event*]
                         ;(.-movementX event*) is undefined in :advanced.
                         (mousemove {:movement-x (aget event* "movementX")
                                     :movement-y (aget event* "movementY")})))

  (add-remove-listener "mouseup"
                       (fn [event*]
                         (mouseup {}))))
