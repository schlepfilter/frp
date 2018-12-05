(ns examples.rx.drag-n-drop
  (:require [cats.core :as m]
            [com.rpl.specter :as s :include-macros true]
            [frp.core :as frp]
            [frp.window :as window]
            [aid.core :as aid]))

(def black "hsl(0, 0%, 0%)")

(def white "hsl(0, 0%, 100%)")

(def drag-start
  (frp/event))

(def drop*
  (frp/event))

(def initialize
  (partial frp/stepper (->> 0
                            repeat
                            (interleave #{:left :page-x :page-y :top})
                            (apply array-map))))

(def origin
  (->> drag-start
       initialize
       (frp/snapshot drop*)
       (m/<$> (fn [[drop** drag-start*]]
                (->> drag-start*
                     (s/transform :left (partial + (- (:page-x drop**)
                                                      (:page-x drag-start*))))
                     (s/transform :top (partial + (- (:page-y drop**)
                                                     (:page-y drag-start*)))))))
       initialize))

(defn drag-n-drop-component
  [{:keys [left top]} height]
  [:div {:on-drop      #(drop* {:page-x (.-pageX %)
                                :page-y (.-pageY %)})
         :on-drag-over #(.preventDefault %)
         :style        {:position "absolute"
                        :top      0
                        :height   height
                        :width    "100%"}}
   [:div {:draggable     true
          :on-drag-start #(drag-start {:left   left
                                       :page-x (.-pageX %)
                                       :page-y (.-pageY %)
                                       :top    top})
          :style         {:background-image    "url(/img/logo.png)"
                          :background-repeat   "no-repeat"
                          :background-position "center"
                          :background-color    black
                          :color               white
                          :height              200
                          :left                left
                          :position            "absolute"
                          :top                 top
                          :width               200}}
    "Drag Me!"]
   [:h1 "Drag and Drop Example"]
   [:p "Example to show coordinating events to perform drag and drop"]])

(def drag-n-drop
  ((aid/lift-a drag-n-drop-component) origin window/inner-height))
