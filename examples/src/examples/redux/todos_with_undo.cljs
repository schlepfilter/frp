(ns examples.redux.todos-with-undo
  (:require [clojure.string :as str]
            [aid.core :as aid]
            [cats.core :as m]
            [com.rpl.specter :as s]
            [frp.clojure.core :as core]
            [frp.core :as frp]
            [frp.window :as window]))

(frp/defe typing addition toggle undo redo view-event)

(aid/defcurried transfer*
  [apath f m]
  (s/setval apath (f m) m))

(def todo
  (frp/snapshot (->> typing
                     (frp/stepper "")
                     ;TODO snapshot addition and time at the same time
                     (frp/snapshot addition)
                     (m/<$> last)
                     (core/remove empty?))
                frp/time))

(def size
  10)

(def todos
  (frp/with-undo size
                 undo
                 redo
                 [todo toggle]
                 ((aid/lift-a (fn [additions m]
                                (map (transfer* s/AFTER-ELEM (comp m
                                                                   last))
                                     additions)))
                   (->> todo
                        core/vector
                        (frp/stepper []))
                   (->> todo
                        (m/<$> last)
                        (m/<> toggle)
                        (core/group-by identity)
                        (m/<$> (partial s/transform* s/MAP-VALS (comp odd?
                                                                      count)))
                        (frp/stepper {})))))

(def view-behavior
  (frp/stepper :all view-event))

(def visible-todos
  ((aid/lift-a (fn [todos* view*]
                 (filter (view* {:all       (constantly true)
                                 :active    last
                                 :completed (complement last)})
                         todos*)))
    todos
    view-behavior))

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
  [[s t active]]
  [:li {:on-click #(toggle t)}
   (if active
     s
     [:del s])])

(aid/defcurried link-component
  [view* k]
  [:a ((aid/case-eval view*
         k identity
         (partial s/setval* :href "#"))
        {:on-click (fn [event*]
                     (.preventDefault event*)
                     (view-event k))})
   (capital k)])

(defn history-component
  [[e s]]
  [:button {:on-click #(e)}
   s])

(defn todos-with-undo-component
  [todos* view*]
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
        (map (link-component view*))
        (sequence-join ", ")
        vec
        (s/setval s/BEGINNING [:p "Show: "]))])

(def todos-with-undo
  ((aid/lift-a todos-with-undo-component) visible-todos view-behavior))

(frp/on (comp aid/funcall
              :prevent-default)
        window/submit)
