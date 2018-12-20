(ns examples.redux.todos-with-undo
  (:require [clojure.string :as str]
            [aid.core :as aid]
            [cats.core :as m]
            [com.rpl.specter :as s]
            [frp.clojure.core :as core]
            [frp.core :as frp :include-macros true]
            [frp.window :as window]))

(frp/defe typing addition toggle undo redo view)

(aid/defcurried transfer*
  [apath f m]
  (s/setval apath (f m) m))

(def todo
  (frp/snapshot (->> typing
                     (frp/stepper "")
                     ;TODO snapshot addition and time at the same time
                     (frp/snapshot addition)
                     (m/<$> second)
                     (core/remove empty?))
                frp/time))

(def todos
  ((aid/lift-a (fn [additions m]
                 (map (transfer* s/AFTER-ELEM (comp m
                                                    last))
                      additions)))
    (->> todo
         core/vector
         (frp/stepper []))
    (->> toggle
         (core/group-by identity)
         (m/<$> (partial s/transform* s/MAP-VALS (comp even?
                                                       count)))
         (frp/stepper {}))))

(defn sequence-join
  [separator coll]
  (->> coll
       (interleave (repeat separator))
       rest))

(def capital
  ;TODO fix cuerdas
  (comp str/capitalize
        name))

(defn todos-with-undo-component
  ;TODO implement this function
  [todos*]
  [:div
   [:form {:on-submit #(addition)}
    [:input {:on-change #(-> %
                             .-target.value
                             typing)}]
    [:button {:type "submit"}
     "Add Todo"]]
   (->> todos*
        (mapv (fn [[s t completed]]
                [:li {:on-click #(toggle t)}
                 (if completed
                   [:del s]
                   s)]))
        (s/setval s/BEFORE-ELEM :ul))
   [:div
    ;TODO extract a function that returns a button component
    [:button {:on-click #(undo)}
     "undo"]
    [:button {:on-click #(redo)}
     "redo"]]
   (->> [:all :active :completed]
        (map (fn [s]
               [:a {:href     "#"
                    :on-click (fn [event*]
                                (.preventDefault event*)
                                (view s))}
                (capital s)]))
        (sequence-join ", ")
        vec
        (s/setval s/BEGINNING [:p "Show: "]))])

(def todos-with-undo
  (m/<$> todos-with-undo-component todos))

(frp/on (comp aid/funcall
              :prevent-default)
        window/submit)
