(ns examples.rx.simple-data-binding
  (:require [aid.core :as aid]
            [cats.core :as m]
            [frp.core :as frp :include-macros]))

(defn partial-name
  [{:keys [event label]}]
  [:div
   [:label label]
   [:input {:on-change   #(-> %
                              .-target.value
                              event)
            :placeholder (str "Enter " label "Name...")}]])

(def first-name
  (frp/event))

(def last-name
  (frp/event))

(defn simple-data-binding-component
  [full-name*]
  [:div
   [:h1 "Simple Data Binding Example"]
   [:p "Show simple concepts of data binding!"]
   [partial-name {:event first-name
                  :label "First"}]
   [partial-name {:event last-name
                  :label "Last"}]
   [:div "Full Name"]
   [:div full-name*]])

(def full-name
  ((aid/lift-a str)
    (frp/stepper "" first-name)
    (frp/behavior " ")
    (frp/stepper "" last-name)))

(def simple-data-binding
  (m/<$> simple-data-binding-component full-name))
