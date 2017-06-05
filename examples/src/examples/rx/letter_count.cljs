(ns examples.rx.letter-count
  (:require [help.core :as help]
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
          [:input {:on-change (fn [event*]
                                (-> event*
                                    .-target.value.length
                                    length))}]]
    [:p message]]])

(def letter-count
  (->> length
       (help/<$> (partial str "length: "))
       (frp/stepper "Start Typing!")
       (help/<$> letter-count-component)))
