(ns examples.cycle.counter
  (:require [aid.core :as aid]
            [aid.unit :as unit]
            [frp.clojure.core :as core]
            [frp.core :as frp]))

(def increment
  (frp/event))

(def decrement
  (frp/event))

(defn counter-component
  [total]
  [:div
   [:button {:on-click #(increment unit/unit)}
    "Increment"]
   [:button {:on-click #(decrement unit/unit)}
    "Decrement"]
   [:p (str "Counter: " total)]])

(def counter
  (->> (aid/<> (aid/<$> (constantly 1) increment)
               (aid/<$> (constantly -1) decrement))
       core/+
       (frp/stepper 0)
       (aid/<$> counter-component)))
