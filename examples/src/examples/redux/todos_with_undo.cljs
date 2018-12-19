(ns examples.redux.todos-with-undo
  (:require [aid.core :as aid]
            [cats.core :as m]
            [com.rpl.specter :as s]
            [frp.clojure.core :as core]
            [frp.core :as frp]
            [frp.window :as window]))

(def typing
  (frp/event))

(def addition
  (frp/event))

(def undo
  (frp/event))

(def redo
  (frp/event))

(def todo
  (->> typing
       (frp/stepper "")
       (frp/snapshot addition)
       (m/<$> second)
       (core/remove empty?)
       (m/<$> ((aid/curry 3 s/setval*) s/AFTER-ELEM))
       (frp/accum [])))

(defn todos-with-undo-component
  ;TODO implement this function
  [todo*]
  [:div
   [:form {:on-submit #(addition)}
    [:input {:on-change #(-> %
                             .-target.value
                             typing)}]
    [:button {:type "submit"}
     "Add Todo"]]
   (->> todo*
        (mapv (partial vector :li))
        (s/setval s/BEFORE-ELEM :ul))
   [:div
    ;TODO extract a function that returns a button component
    [:button {:on-click #(undo)}
     "undo"]
    [:button {:on-click #(redo)}
     "redo"]]])

(def todos-with-undo
  (->> todo
       (frp/stepper [])
       (m/<$> todos-with-undo-component)))

(frp/on (comp aid/funcall
              :prevent-default)
        window/submit)
