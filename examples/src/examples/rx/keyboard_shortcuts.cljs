(ns examples.rx.keyboard-shortcuts
  (:require [cats.core :as m]
            [cljsjs.mousetrap]
            [com.rpl.specter :as s]
            [frp.core :as frp]
            [frp.clojure.core :as core]
            [aid.core :as aid]
            [clojure.string :as str]))

(def typing
  (frp/event))

(def registration
  (frp/event))

(def trigger
  (frp/event))

(def counter
  (->> trigger
       (m/<> registration)
       (core/group-by identity)
       (m/<$> (partial s/transform* s/MAP-VALS (comp dec
                                                     count)))))

(defn keyboard-shortcuts-component
  [typing* counter*]
  [:div
   [:input {:type        "text"
            :placeholder "ctrl+alt+d"
            :value       typing*
            :on-change   #(-> %
                              .-target.value
                              typing)}]
   [:button {:on-click #(registration typing*)}
    "Add"]
   [:p "Keyboard shortcuts"]
   (->> counter*
        (s/transform s/MAP-VALS str)
        (mapv (comp (partial vector :li)
                    (partial str/join ": ")))
        (s/setval s/BEFORE-ELEM :ul))])

(def keyboard-shortcuts
  ((aid/lift-a keyboard-shortcuts-component)
    (frp/stepper "" typing)
    (frp/stepper {} counter)))

(frp/on (fn [registration*]
          (js/Mousetrap.bind registration* #(trigger registration*)))
        registration)
