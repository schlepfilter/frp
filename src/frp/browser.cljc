(ns frp.browser
  (:require [clojure.string :as str]
            [aid.core :as aid]
            [cuerdas.core :as cuerdas]
            [goog.object :as object]
            [oops.core :refer [oget+]]
            [frp.derived :as derived]
            [frp.io :as io]
            [frp.primitives.behavior :as behavior]
            [frp.primitives.event :as event]))

(defn make-redef-event
  [e]
  #(behavior/redef e
                   (derived/event)))

(def get-event
  (comp (io/effect (comp behavior/register!
                         make-redef-event))
        event/->Event))

(defn add-remove-listener
  [target event-type listener]
  #?(:cljs
     (do
       (.addEventListener target event-type listener)
       (swap!
         event/network-state
         (event/append-cancellation (fn [_]
                                      (.removeEventListener target
                                                            event-type
                                                            listener)))))))

(defn listen
  [f e]
  #?(:cljs
     (behavior/register!
       #(add-remove-listener (aget js/window (-> e
                                                 :id
                                                 namespace
                                                 (str/split #"\.")
                                                 last))
                             (-> e
                                 :id
                                 name)
                             (comp e
                                   f))))
  e)

(aid/defcurried make-redef-behavior
  [f b]
  #(behavior/redef b (f)))

(defn get-behavior
  [f k]
  (->> k
       behavior/->Behavior
       (io/effect (comp behavior/register!
                        (make-redef-behavior f)))))

(def get-caller-keyword
  #(->> %
        (str *ns* "/")
        keyword))

#?(:clj
   (do (defmacro defevent
         ([expr]
          `(def ~expr
             (get-event ~(get-caller-keyword expr))))
         ([expr f]
          `(listen ~f (defevent ~expr))))

       (defmacro defbehavior
         ([expr f]
          `(def ~expr
             (get-behavior ~f ~(get-caller-keyword expr)))))))

(defn convert
  [x]
  (->> x
       object/getKeys
       (mapcat (juxt (comp keyword
                           cuerdas/kebab)
                     #(case (-> x
                                (oget+ %)
                                goog/typeOf)
                        "function" (partial js-invoke x %)
                        (oget+ x %))))
       (apply hash-map)))

