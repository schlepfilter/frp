(ns examples.rx.letter-count
  (:require [cats.core :as m]
            [frp.core :as frp]))

(def length
  (frp/event))

(defn letter-count-component
  [message]
  [:div
   [:h1 "Letter Counting Example"]
   [:p "Example to show getting the current length of the input."]
   [:div [:p
          "Text buffer: "
          [:input {:on-change #(-> %
                                   .-target.value.length
                                   length)}]]
    [:p message]]])

(def letter-count
  (frp/transparent (->> length
                        (str "length: ")
                        (frp/stepper "Start Typing!")
                        letter-count-component)))
