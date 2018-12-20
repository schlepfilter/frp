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

(defn todo-component
  [[s t completed]]
  [:li {:on-click #(toggle t)}
   (if completed
     [:del s]
     s)])

(defn link-component
  [s]
  [:a {:href     "#"
       :on-click (fn [event*]
                   (.preventDefault event*)
                   (view s))}
   (capital s)])

(defn history-component
  [[e s]]
  [:button {:on-click #(e)}
   s])

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
        (mapv todo-component)
        (s/setval s/BEFORE-ELEM :ul))
   (->> [[undo "undo"] [redo "redo"]]
        (mapv history-component)
        (s/setval s/BEFORE-ELEM :div))
   (->> [:all :active :completed]
        (map link-component)
        (sequence-join ", ")
        vec
        (s/setval s/BEGINNING [:p "Show: "]))])

(def todos-with-undo
  (m/<$> todos-with-undo-component todos))

(frp/on (comp aid/funcall
              :prevent-default)
        window/submit)
