(ns examples.cycle.counter
  (:require [aid.core :as aid]
            [aid.unit :as unit]
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
   ;TODO make event accept zero argument
   [:button {:on-click #(increment unit/unit)}
    "Increment"]
   [:button {:on-click #(decrement unit/unit)}
    "Decrement"]
   [:p (str "Counter: " total)]])

(def counter
  (->> (m/<> (aid/<$ 1 increment) (aid/<$ -1 decrement))
       core/+
       (frp/stepper 0)
       (m/<$> counter-component)))
