(ns frp.browser
  (:require [clojure.string :as str]
            [aid.core :as aid]
            [cats.core :as m]
            [cuerdas.core :as cuerdas]
            #?@(:cljs
                [[goog.object :as object]
                 [oops.core :refer [oget+]]])
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
  #?@(:cljs
      [(.addEventListener target event-type listener)
       (swap! event/network-state
              (event/append-cancellation (fn [_]
                                           (.removeEventListener target
                                                                 event-type
                                                                 listener))))]))

(def get-property-name
  (comp last
        (partial (aid/flip str/split) #"\.")
        namespace))

(defn listen
  [f e]
  #?(:cljs
     (behavior/register!
       #(add-remove-listener (oget+ js/window (-> e
                                                  :id
                                                  get-property-name))
                             (-> e
                                 :id
                                 name)
                             (comp e
                                   f))))
  e)

(def disqualify
  (comp keyword
        name))

(aid/defcurried make-redef-behavior
  [f k b]
  #?(:cljs
     #(behavior/redef
        b
        (->> (f)
             (m/<$> (disqualify k))
             (behavior/stepper (->> k
                                    disqualify
                                    cuerdas/camel
                                    (oget+ js/window
                                           (get-property-name k))))))))

(defn get-behavior
  [f k]
  #?(:cljs
     (->> k
          behavior/->Behavior
          (io/effect (comp behavior/register!
                           (make-redef-behavior f k))))))

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
         ([expr e]
          `(def ~expr
             (get-behavior (fn []
                             ~e)
                           ~(get-caller-keyword expr)))))))

(def memoized-keyword
  (memoize cuerdas/keyword))

#?(:cljs
   (defn convert
     [x]
     (->> x
          object/getKeys
          ;Not memoizing keyword is visibly slower.
          (mapcat (juxt memoized-keyword
                        #(case (-> x
                                   (oget+ %)
                                   goog/typeOf)
                           "function" (partial js-invoke x %)
                           (oget+ x %))))
          (apply hash-map))))
