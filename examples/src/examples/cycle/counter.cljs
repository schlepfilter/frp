(ns examples.cycle.counter
  (:require [aid.core :as aid]
            [cats.core :as m]
            [frp.clojure.core :as core]
            [frp.core :as frp]))

(def increment
  (frp/event))

(def decrement
  (frp/event))

(defn counter-component
  [total]
  [:div
   [:button {:on-click #(increment)}
    "Increment"]
   [:button {:on-click #(decrement)}
    "Decrement"]
   [:p (str "Counter: " total)]])

(def counter
  (->> (m/<> (aid/<$ 1 increment) (aid/<$ -1 decrement))
       core/+
       (frp/stepper 0)
       (m/<$> counter-component)))
