(ns examples.re-frame.simple
  (:require [aid.core :as aid]
            [cats.core :as m]
            [cljs-time.coerce :as c]
            [cljs-time.format :as f]
            [frp.core :as frp]
            [frp.time :as time]))

(frp/defe color-event)

(def clock-behavior
  ;TODO expose to-real-time in frp.core
  (m/<$> (comp (partial f/unparse (f/formatters :hour-minute-second))
               c/from-long
               deref
               time/to-real-time) frp/time))

(def color-behavior
  (frp/stepper "#f88" color-event))

(defn simple-component
  [clock* color*]
  [:div
   [:h1 "Hello world, it is now"]
   [:div {:style {:color color*}}
    clock*]
   [:input {:on-change #(-> %
                            .-target.value
                            color-event)
            :value     color*}]])

(def simple
  ((aid/lift-a simple-component) clock-behavior color-behavior))
