(ns examples.cycle.reverse-text
  (:require [help.core :as help]
            [help.unit :as unit]
            [frp.clojure.core :as core]
            [frp.core :as frp]))

(def key-stroke
  (frp/event))

(def reversed-text
  (->> key-stroke
       (help/<$> reverse)
       (frp/stepper "")))

(def style
  {:width        "100%"
   :height       "40px"
   :padding      "10px 0"
   :text-align   "center"
   :font-size    "2em"
   :font-family  "Helvetica"})

(defn text-component
  [reversed-text*]
  [:div
   [:input {:on-change   #(-> % .-target .-value key-stroke)
            :type        "text"
            :placeholder "Text to reverse"
            :style       style}]
   [:p {:style style} reversed-text*]])

(def reverse-text
  (help/<$> text-component reversed-text))
