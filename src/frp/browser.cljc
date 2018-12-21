(ns frp.browser
  (:require [clojure.string :as str]
            [aid.core :as aid]
            [cats.core :as m]
            [cuerdas.core :as cuerdas]
            #?@(:cljs [[goog.object :as object]
                       [oops.core :refer [oget+]]])
            [frp.derived :as derived]
            [frp.primitives.behavior :as behavior]
            [frp.primitives.event :as event])
  #?(:cljs (:require-macros frp.browser)))

(defn make-redef-event
  [e]
  #(behavior/redef e
                   (derived/event)))

(def get-event
  (comp (event/effect (comp behavior/register!
                            make-redef-event))
        event/->Event))

(defn add-remove-listener
  [target event-type listener]
  #?@(:cljs
      [(.addEventListener target event-type listener)
       (swap! event/universe-state
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
          (event/effect (comp behavior/register!
                              (make-redef-behavior f k))))))

(def get-caller-keyword
  #(->> %
        (str *ns* "/")
        keyword))

(defmacro defevent
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
                    ~(get-caller-keyword expr)))))

(def memoized-keyword
  (memoize cuerdas/keyword))

(defn convert
  [x]
  #?(:cljs (->> x
                object/getKeys
                ;Doing memoization is visibly faster.
                (mapcat (juxt memoized-keyword
                              #(case (-> x
                                         (oget+ %)
                                         goog/typeOf)
                                 "function" (partial js-invoke x %)
                                 (oget+ x %))))
                (apply hash-map))))

(defmacro make-convert-merge
  [x]
  `#(merge (convert %) (convert ~x)))
