(ns examples.rx.keyboard-shortcuts
  (:require [clojure.string :as str]
            [aid.core :as aid]
            [cats.core :as m]
            [cljsjs.mousetrap]
            [com.rpl.specter :as s]
            [frp.core :as frp]
            [frp.clojure.core :as core]))

(def combine
  (comp (partial str/join "+")
        vector))

(def default
  (combine "ctrl" "alt" "d"))

(def typing
  (frp/event))

(def registration
  (frp/event default (combine "ctrl" "alt" "s") "trash"))

(def trigger
  (frp/event))

(def counter
  (->> trigger
       (m/<> (core/distinct registration))
       (core/group-by identity)
       (m/<$> (partial s/transform* s/MAP-VALS (comp dec
                                                     count)))))

(defn keyboard-shortcuts-component
  [typing* counter*]
  [:div
   [:input {:type        "text"
            :placeholder default
            :value       typing*
            :on-change   #(-> %
                              .-target.value
                              typing)}]
   [:button {:on-click #(registration typing*)}
    "Add"]
   [:p "Keyboard shortcuts:"]
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
