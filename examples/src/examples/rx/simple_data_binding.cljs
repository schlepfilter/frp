(ns examples.rx.simple-data-binding
  (:require [clojure.string :as str]
            [cats.core :as m]
            [examples.helpers :as helpers]))

(defn partial-name
  [{:keys [event label]}]
  [:div
   [:label (str label " Name")]
   [:input {:on-change   #(-> %
                              .-target.value
                              event)
            :placeholder (str/join " " ["Enter" label "Name..."])}]])

(def first-name-component
  (partial-name {:event helpers/first-name
                 :label "First"}))

(def last-name-component
  (partial-name {:event helpers/last-name
                 :label "Last"}))

(defn simple-data-binding-component
  [full-name*]
  [:div
   [:h1 "Simple Data Binding Example"]
   [:p "Show simple concepts of data binding!"]
   first-name-component
   last-name-component
   [:div "Full Name"]
   [:div full-name*]])

(def simple-data-binding
  (m/<$> simple-data-binding-component helpers/full-name))
