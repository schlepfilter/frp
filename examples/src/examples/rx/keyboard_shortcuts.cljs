(ns examples.rx.keyboard-shortcuts
  (:require [cljsjs.mousetrap]
            [cats.core :as m]
            [frp.core :as frp]))

(def typing
  (frp/event))

(def registration
  (frp/event))

(def trigger
  (frp/event))

(defn keyboard-shortcuts-component
  [x]
  [:div
   [:input {:type        "text"
            :placeholder "ctrl+alt+d"
            :value       x
            :on-change   #(-> %
                              .-target.value
                              typing)}]
   [:button {:on-click #(registration x)}
    "Add"]
   [:p "Keyboard shortcuts"]
   [:ul]])

(def keyboard-shortcuts
  (m/<$> keyboard-shortcuts-component (frp/stepper "" typing)))

(frp/on (fn [registration*]
          (js/Mousetrap.bind registration* #(trigger registration*)))
        registration)
