(ns examples.rx.drag-n-drop
  (:require [help.core :as help]
            [help.unit :as unit]
            [frp.clojure.core :as core]
            [frp.core :as frp]
            [frp.window :as window]))

(def black "hsl(0, 0%, 0%)")

(def white "hsl(0, 0%, 100%)")

(def mousedown
  (frp/event))

(def drag
  (->> (help/<> (help/<$> (constantly true) mousedown)
                (help/<$> (constantly false) window/mouseup))
       (frp/stepper false)))

(def movement
  (->> drag
       (frp/snapshot window/mousemove)
       (core/filter second)
       (help/<$> first)))

(def get-one-dimension
  (comp (partial frp/stepper 0)
        core/+
        (partial (help/flip help/<$>) movement)))

(def left
  (get-one-dimension :movement-x))

(def top
  (get-one-dimension :movement-y))

(def origin
  ;TODO infer the number of arguments from fn
  ((help/lift-a (fn [left* top*]
                  {:left left*
                   :top  top*}))
    left
    top))

(defn drag-n-drop-component
  [{:keys [left top]}]
  [:div
   [:div {:on-mouse-down (fn [_]
                           (mousedown unit/unit))
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
  (help/<$> drag-n-drop-component origin))
