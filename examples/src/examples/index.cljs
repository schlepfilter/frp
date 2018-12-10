(ns examples.index
  (:require [bidi.bidi :as bidi]
            [com.rpl.specter :as s]
            [frp.core :as frp]
            [frp.history :as history]
            [examples.cycle.autocomplete-search :as autocomplete-search]
            [examples.cycle.bmi-naive :as bmi-naive]
            [examples.cycle.checkbox :as checkbox]
            [examples.cycle.counter :as counter]
            [examples.cycle.http-search-github :as http-search-github]
            [examples.intro :as intro]
            [examples.rx.drag-n-drop :as drag-n-drop]
            [examples.rx.letter-count :as letter-count]
            [examples.rx.simple-data-binding :as simple-data-binding]))

(def route-function
  {:autocomplete-search autocomplete-search/autocomplete-search
   :bmi-naive           bmi-naive/bmi-naive
   :checkbox            checkbox/checkbox
   :counter             counter/counter
   :drag-n-drop         drag-n-drop/drag-n-drop
   :http-search-github  http-search-github/http-search-github
   :intro               intro/intro
   :letter-count        letter-count/letter-count
   :simple-data-binding simple-data-binding/simple-data-binding})

(def route-keywords
  (keys route-function))

(def example-route
  (zipmap (map name route-keywords) route-keywords))

(def route
  ["/" (merge {"" :index} example-route)])

(defn example-component
  [path]
  [:a {:href     path
       :on-click (fn [event*]
                   (.preventDefault event*)
                   (history/push-state {} {} path))}
   [:li (subs path 1)]])

(def index-component
  (->> route-keywords
       (mapv (comp example-component
                   (partial bidi/path-for route)))
       (s/setval s/BEGINNING [:ul])))

(def index
  (frp/behavior index-component))
