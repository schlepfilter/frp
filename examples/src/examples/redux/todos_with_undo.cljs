(ns examples.redux.todos-with-undo
  (:require [cats.core :as m]
            [frp.core :as frp]
            [frp.window :as window]
            [aid.core :as aid]))

(frp/on (comp aid/funcall
              :prevent-default)
        window/submit)

(def typing
  (frp/event))

(defn todos-with-undo-component
  ;TODO implement this function
  []
  [:div
   [:form
    [:input {:on-change #(-> %
                             .-target.value
                             typing)}]
    [:button
     {:type "submit"}
     "Add Todo"]]])

(def todos-with-undo
  (m/<$> todos-with-undo-component (frp/stepper "" typing)))
