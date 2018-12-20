(ns examples.rx.keyboard-shortcuts
  (:require [clojure.string :as str]
            [cats.core :as m]
            [cljsjs.mousetrap]
            [com.rpl.specter :as s]
            [frp.core :as frp]
            [frp.clojure.core :as core]))

(def combine
  (comp (partial str/join "+")
        vector))

(def placeholder
  (combine "ctrl" "alt" "d"))

(def typing
  (frp/event))

(def addition
  (frp/event))

(def registration
  (->> typing
       (frp/stepper "")
       (frp/snapshot addition)
       (m/<$> second)
       (m/<> (frp/event placeholder (combine "ctrl" "alt" "s") "trash"))
       core/distinct))

(def trigger
  (frp/event))

(def counter
  (->> trigger
       (m/<> registration)
       (core/group-by identity)
       (m/<$> (partial s/transform* s/MAP-VALS (comp dec
                                                     count)))))

(defn keyboard-shortcuts-component
  [counter*]
  [:div
   [:input {:on-change   #(-> %
                              .-target.value
                              typing)
            :placeholder placeholder}]
   [:button {:on-click #(addition)}
    "Add"]
   [:p "Keyboard shortcuts:"]
   (->> counter*
        (s/transform s/MAP-VALS str)
        (mapv (comp (partial vector :li)
                    (partial str/join ": ")))
        (s/setval s/BEFORE-ELEM :ul))])

(def keyboard-shortcuts
  (->> counter
       (frp/stepper {})
       (m/<$> keyboard-shortcuts-component)))

(frp/on (fn [registration*]
          (js/Mousetrap.bind registration* #(trigger registration*)))
        registration)
