(ns examples.rx.data-binding
  (:require [aid.core :as aid]
            [com.rpl.specter :as s]
            [frp.clojure.core :as core]
            [frp.core :as frp]
            [examples.helpers :as helpers]
            [cats.core :as m]))

(frp/defe click)

(def counter
  (core/count click))

(def initial-click-difference
  [0 0])

(def click-difference
  (->> frp/time
       (m/<$> deref)
       (frp/snapshot counter)
       (m/<> (frp/event initial-click-difference))
       (core/partition 2 1)
       (m/<$> (fn [[past current]]
                (s/transform s/LAST
                             (partial (aid/flip -) (last past))
                             current)))
       (frp/stepper initial-click-difference)))

(defn partial-name
  [{:keys [event label placeholder]}]
  [:div
   [:label (str label " Name")]
   [:input {:on-change   #(-> %
                              .-target.value
                              event)
            :placeholder placeholder}]])

(def first-name-component
  (partial-name {:event       helpers/first-name
                 :label       "First"
                 :placeholder "Reactive"}))

(def last-name-component
  (partial-name {:event       helpers/last-name
                 :label       "Last"
                 :placeholder "Extensions"}))

(defn full-name-component
  [full-name*]
  [:div
   first-name-component
   last-name-component
   [:div "Full Name"]
   [:div full-name*]])

(defn click-component
  [counter* difference]
  [:div
   [:button {:on-click #(click)}
    "click"]
   [:div "Times Clicked:"]
   [:div counter*]
   [:div "Times between clicks:"]
   [:div difference]])

(defn data-binding-component
  [full-name* coll]
  [:div
   [full-name-component full-name*]
   (->> coll
        (s/setval s/BEFORE-ELEM click-component)
        vec)])

(def data-binding
  ((aid/lift-a data-binding-component) helpers/full-name click-difference))
