(ns examples.rx.drag-n-drop
  (:require [aid.core :as aid]
            [cats.core :as m]
            [clojure.set :as set]
            [com.rpl.specter :as s]
            [frp.clojure.core :as core]
            [frp.core :as frp]
            [frp.window :as window]))

(def initialize
  (partial frp/stepper (s/setval (s/multi-path :page-x :page-y) 0 {})))

(def origin
  (->> window/dragstart
       initialize
       (frp/snapshot window/drop)
       (m/<$> (partial apply merge-with -))
       (core/merge-with +)
       initialize
       (m/<$> (partial (aid/flip set/rename-keys) {:page-x :left
                                                   :page-y :top}))))

(def square-height
  200)

(defn drag-n-drop-component
  [origin*]
  [:div {:on-drag-over #(.preventDefault %)
         :style        {:position "absolute"
                        :top      0
                        :height   "100%"
                        :width    "100%"}}
   [:div {:draggable true
          :style     (merge origin*
                            {:background-image    "url(/img/logo.png)"
                             :background-repeat   "no-repeat"
                             :background-position "center"
                             :background-color    "black"
                             :color               "white"
                             :height              square-height
                             :position            "absolute"
                             :width               square-height})}
    "Drag Me!"]
   [:h1 "Drag and Drop Example"]
   [:p "Example to show coordinating events to perform drag and drop"]])

(def drag-n-drop
  (m/<$> drag-n-drop-component origin))
