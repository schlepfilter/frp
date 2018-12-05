(ns examples.rx.drag-n-drop
  (:require [aid.core :as aid]
            [cats.core :as m]
            [com.rpl.specter :as s :include-macros true]
            [frp.core :as frp]
            [frp.window :as window]))

(def black "hsl(0, 0%, 0%)")

(def white "hsl(0, 0%, 100%)")

(def drag-start
  (frp/event))

(def drop*
  (frp/event))

(def initialize
  (partial frp/stepper
           (s/setval (s/multi-path :left :page-x :page-y :top) 0 {})))

(def origin
  (->> drag-start
       initialize
       (frp/snapshot drop*)
       (m/<$> #(->> %
                    second
                    (s/transform :left (partial + (->> %
                                                       (map :page-x)
                                                       (apply -))))
                    (s/transform :top (partial + (->> %
                                                      (map :page-y)
                                                      (apply -))))))
       initialize))

(defn drag-n-drop-component
  [origin* height]
  [:div {:on-drop      #(drop* {:page-x (.-pageX %)
                                :page-y (.-pageY %)})
         :on-drag-over #(.preventDefault %)
         :style        {:position "absolute"
                        :top      0
                        :height   height
                        :width    "100%"}}
   [:div {:draggable     true
          :on-drag-start #(->> {:page-x (.-pageX %)
                                :page-y (.-pageY %)}
                               (merge origin*)
                               (drag-start))
          :style         (merge origin*
                                {:background-image    "url(/img/logo.png)"
                                 :background-repeat   "no-repeat"
                                 :background-position "center"
                                 :background-color    black
                                 :color               white
                                 :height              200
                                 :position            "absolute"
                                 :width               200})}
    "Drag Me!"]
   [:h1 "Drag and Drop Example"]
   [:p "Example to show coordinating events to perform drag and drop"]])

(def drag-n-drop
  ((aid/lift-a drag-n-drop-component) origin window/inner-height))
