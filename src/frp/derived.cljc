(ns frp.derived
  (:require [clojure.walk :as walk]
            [aid.core :as aid]
            [aid.unit :as unit]
            [cats.core :as m]
            [com.rpl.specter :as s]
            [linked.core :as linked]
            #?(:clj [riddley.walk :as riddley])
            [frp.clojure.core :as core]
            [frp.io :as io]
            [frp.primitives.behavior :as behavior]
            [frp.primitives.event :as event]
            [frp.time :as time])
  #?(:cljs (:require-macros frp.derived)))

(defn event
  ([& as]
   (aid/casep as
     empty? (event/mempty)
     (->> as
          (map event/pure)
          (apply m/<>)))))

(defmacro defe
  [& names]
  `(do ~@(map (fn [x#]
                `(def ~x#
                   (event)))
              names)))

(def behavior?
  (partial instance? frp.primitives.behavior.Behavior))

(def behavior
  behavior/pure)

(def behaviorize
  (aid/if-else behavior?
               behavior))

(def eventize
  (aid/if-else event/event?
               event))

(def multiton?
  (comp (partial < 1)
        count))

(def has-argument?
  (aid/build and
             seq?
             multiton?))

(defn make-only?
  [x y]
  (aid/build and
             (partial some x)
             (complement (partial some y))))

(def event-only?
  (make-only? event/event? behavior?))

(def behavior-only?
  (make-only? behavior? event/event?))

(defn transparent*
  [f & more]
  (->> more
       (map (aid/casep more
              event-only? eventize
              behavior-only? behaviorize
              identity))
       (apply ((aid/casep more
                 (aid/build or
                            event-only?
                            behavior-only?)
                 aid/lift-a
                 identity)
                f))))

;The reader conditional avoids the following warning.
;WARNING: Use of undeclared Var clojure.walk/macroexpand-all
#?(:clj
   (defmacro transparent
     [expr]
     (->> expr
          ;TODO macroexpand expr when ClojureScript starts supporting runtime macro expansion
          ;macroexpand is only intended as a REPL utility
          ;https://cljs.github.io/api/cljs.core/macroexpand
          walk/macroexpand-all
          (walk/postwalk #(aid/casep %
                            has-argument? `(apply transparent* ~(vec %))
                            %)))))

(def accum
  (partial core/reduce (aid/flip aid/funcall)))

(def switcher
  (comp m/join
        behavior/stepper))

;TODO move this function to aid
(aid/defcurried transfer*
  [apath f m]
  (s/setval apath (f m) m))

(def SECOND
  (s/nthpath 1))

(def set-non-action
  (partial s/setval* s/FIRST true))

(def sfirst
  (comp first
        second))

(defn get-undo-redo
  [size undo redo network]
  (->> network
       (m/<$> #(aid/if-else (comp (partial (aid/flip aid/funcall) %)
                                  set
                                  flatten
                                  rest)
                            (comp (partial s/setval* s/FIRST false)
                                  (partial s/setval* s/LAST [])
                                  (partial s/transform*
                                           SECOND
                                           (comp (partial take (inc size))
                                                 (partial s/setval*
                                                          s/BEFORE-ELEM
                                                          %))))))
       ;TODO extract a function
       (m/<> (aid/<$ (aid/if-then (comp multiton?
                                        second)
                                  (comp set-non-action
                                        (partial s/transform*
                                                 SECOND
                                                 rest)
                                        (transfer* [s/LAST s/BEFORE-ELEM]
                                                   sfirst)))
                     undo)
             (aid/<$ (aid/if-else (comp empty?
                                        last)
                                  (comp set-non-action
                                        (partial s/transform*
                                                 s/LAST
                                                 rest)
                                        (transfer* [SECOND s/BEFORE-ELEM]
                                                   (comp first
                                                         last))))
                     redo))
       (accum [false [] []])
       (core/filter first)
       core/dedupe
       (m/<$> sfirst)))

(def prefix
  (gensym))

(def get-alias
  (comp symbol
        (partial str prefix)))

(def get-event-alias
  (comp (partial apply array-map)
        (partial mapcat (fn [x]
                          [x (get-alias x)]))))

(defn on-action
  [action]
  `(io/on ~(get-alias action) ~action))

(def on-actions
  (partial map on-action))

#?(:clj (defn alias-expression
          [actions expr]
          (->> actions
               get-event-alias
               (repeat 2)
               (s/setval s/AFTER-ELEM expr)
               (apply riddley/walk-exprs))))

(defn get-result
  [history size undo redo actions initial-result inner-result]
  (let [network (event)
        outer-result (event)]
    (io/on outer-result inner-result)
    (aid/casep inner-result
      event/event?
      (->> inner-result
           (io/on (fn [_]
                    (network @history))))
      (->> actions
           (apply m/<>)
           (aid/<$ true)
           (m/<> (aid/<$ false (m/<> undo redo)))
           (behavior/stepper true)
           ((aid/lift-a vector) inner-result)
           (io/on (fn [[_ action]]
                    (if action
                      (network @history))))))
    (->> network
         (get-undo-redo size undo redo)
         (io/on history))
    (aid/casep inner-result
      event/event? outer-result
      ((aid/lift-a (fn [t initial-result* outer-result*]
                     (aid/case-eval t
                       time/epoch initial-result*
                       outer-result*)))
        behavior/time
        initial-result
        (behavior/stepper unit/unit outer-result)))))
;This definition may leak memory because of fmapping behavior.
;(defn get-result
;  [history size undo redo actions initial-result inner-result]
;  (let [network (event)
;        outer-result (event)]
;    (->> actions
;         (apply m/<>)
;         (aid/<$ true)
;         (m/<> (->> redo
;                    (m/<> undo)
;                    (aid/<$ false)))
;         (behavior/stepper true)
;         ((aid/casep inner-result
;            event/event? event/snapshot
;            (aid/lift-a vector))
;           inner-result)
;         (io/on (fn [[inner-result* action]]
;                  (outer-result inner-result*)
;                  (if action
;                    (network @history)))))
;    (io/on #(if (not= (:occs @history) (:occs %))
;              (history %))
;           (get-state size undo redo network))
;    (aid/casep inner-result
;      event/event? outer-result
;      (->> outer-result
;           (m/<$> behavior)
;           (switcher initial-result)))))

(aid/defcurried get-binding
  [event* action]
  [(get-alias action) event*])

(defn get-bindings
  [event* actions]
  (mapcat (get-binding event*) actions))

#?(:clj
   (defmacro with-undo
     ;TODO make actions optional for Clojure
     ;TODO make actions optional for ClojureScript when ClojureScript supports dynamic macro expansion with advanced optimizations
     ;TODO deal with the arity in a function
     ([undo actions expr]
      `(with-undo event/positive-infinity ~undo (event) ~actions ~expr))
     ([x y actions expr]
      (aid/casep x
        number? `(with-undo ~x ~y (event) ~actions ~expr)
        `(with-undo event/positive-infinity ~x ~y ~actions ~expr)))
     ([size undo redo actions expr]
      (potemkin/unify-gensyms
        `(let [history## (event/network)
               ~@(get-bindings `(event/with-network history##
                                                    (event))
                               actions)]
           ~@(on-actions actions)
           (get-result
             history##
             ~size
             ~undo
             ~redo
             (event/with-network history## ~actions)
             ~expr
             (event/with-network history##
                                 ~(alias-expression actions expr))))))))
