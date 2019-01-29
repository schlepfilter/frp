(ns examples.rx.data-binding
  (:require [aid.core :as aid]
            [cats.core :as m]
            [com.rpl.specter :as s]
            [frp.clojure.core :as core]
            [frp.core :as frp]
            [examples.helpers :as helpers]))

(frp/defe click)

(def counter
  (core/count click))

(def initial-click-difference
  [0 0])

(def counter-difference
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

(defn counter-component
  [[counter* difference]]
  [:div
   [:button {:on-click #(click)}
    "click"]
   [:div "Times Clicked:"]
   [:div counter*]
   [:div "Times between clicks:"]
   [:div difference]])

(defn data-binding-component
  [full-name* counter-difference*]
  [:div
   [:h1 "TKO - Technical Knockout"]
   [:p
    "Inspired by "
    [:a {:href "https://github.com/cwharris/rxjs-splash"}
     "RxJS-Splash"]
    " and "
    [:a {:href "http://knockoutjs.com/"}
     "Knockout.js"]]
   full-name*
   counter-difference*])

(def data-binding
  (frp/transparent
    (data-binding-component (full-name-component helpers/full-name)
                            (counter-component counter-difference))))
