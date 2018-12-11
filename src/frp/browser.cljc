(ns frp.browser
  (:require [clojure.string :as str]
            [aid.core :as aid]
            [frp.derived :as derived]
            [frp.primitives.behavior :as behavior]
            [frp.primitives.event :as event]))

(aid/defcurried effect
                [f x]
                (f x)
                x)

(defn make-redef-event
  [e]
  #(behavior/redef e
                   (derived/event)))

(def get-event
  (comp (effect (comp behavior/register!
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
  (effect (comp behavior/register!
                (make-redef-behavior f))
          (behavior/->Behavior k)))

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

