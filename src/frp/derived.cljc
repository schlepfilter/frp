(ns frp.derived
  (:require [clojure.walk :as walk]
            [aid.core :as aid]
            [cats.core :as m]
            [com.rpl.specter :as s]
            #?(:clj [riddley.walk :as riddley])
            [frp.clojure.core :as core]
            [frp.io :as io]
            [frp.primitives.behavior :as behavior]
            [frp.primitives.event :as event])
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

(defn get-state
  [size undo redo network]
  (->> network
       (m/<$> #(aid/if-else (comp (partial (aid/flip aid/funcall) %)
                                  set
                                  flatten)
                            (comp (partial s/setval* s/LAST [])
                                  (partial s/transform*
                                           s/FIRST
                                           (comp (partial take (inc size))
                                                 (partial s/setval*
                                                          s/BEFORE-ELEM
                                                          %))))))
       ;TODO extract a function
       (m/<> (aid/<$ (aid/if-then (comp multiton?
                                        first)
                                  (comp (partial s/transform*
                                                 s/FIRST
                                                 rest)
                                        (transfer* [s/LAST s/BEFORE-ELEM]
                                                   ffirst)))
                     undo)
             (aid/<$ (aid/if-else (comp empty?
                                        last)
                                  (comp (partial s/transform*
                                                 s/LAST
                                                 rest)
                                        (transfer* [s/FIRST s/BEFORE-ELEM]
                                                   (comp first
                                                         last))))
                     redo))
       (accum [[] []])
       (core/remove (comp empty?
                          first))
       (m/<$> ffirst)
       core/dedupe))

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

(defn alias-expression
  [actions expr]
  (->> actions
       get-event-alias
       (repeat 2)
       (s/setval s/AFTER-ELEM expr)
       (apply riddley/walk-exprs)))

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
(defn get-result
  [history size undo redo actions inner-result]
  (let [network (event)
        outer-result (event)]
    (->> actions
         (apply m/<>)
         (aid/<$ true)
         (m/<> (aid/<$ false (m/<> undo redo)))
         (behavior/stepper true)
         ((aid/casep inner-result
            event/event? event/snapshot
            (aid/lift-a vector))
           inner-result)
         (io/on (fn [[inner-result* action]]
                  (outer-result inner-result*)
                  (if action
                    ;TODO don't use @
                    (network @history)))))
    ;TODO don't use occs
    (io/on #(if (not= (:occs @history) (:occs %))
              (history %))
           (get-state size undo redo network))
    (aid/casep inner-result
      event/event? outer-result
      (behavior/stepper @inner-result outer-result))))

(aid/defcurried get-binding
  [event* action]
  [(get-alias action) event*])

(defn get-bindings
  [event* actions]
  (mapcat (get-binding event*) actions))

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
        (get-result history##
                    ~size
                    ~undo
                    ~redo
                    ~actions
                    (event/with-network history##
                                        ~(alias-expression actions expr)))))))
