(ns frp.derived
  (:require [clojure.set :as set]
            [clojure.walk :as walk]
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
                   (event/mempty)))
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

(aid/defcurried transfer*
  [apath f m]
  (s/setval apath (f m) m))

(def singleton?
  (comp (partial = 1)
        count))

(defn get-state
  [size undo redo history network*]
  (->> network*
       (m/<$> (fn [network*]
                (aid/if-else
                  (comp (partial (aid/flip aid/funcall) network*)
                        set
                        flatten)
                  (comp (partial s/setval* s/LAST [])
                        (partial s/transform*
                                 s/FIRST
                                 (comp (partial take (inc size))
                                       (partial s/setval*
                                                s/BEFORE-ELEM
                                                network*)))))))
       ;TODO extract a function
       (m/<> (aid/<$ (aid/if-else (comp singleton?
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
       (accum [[@history] []])
       core/dedupe
       (m/<$> ffirst)))

(defn get-event-alias
  [actions history]
  (->> #(event/with-network history
                            (event/mempty))
       repeatedly
       (zipmap actions)))

(defn with-undo*
  [size undo redo history event-alias result*]
  (let [network (event/mempty)
        result (event/mempty)]
    (do (->> event-alias
             set/map-invert
             (run! (partial apply io/on)))
        (io/on result result*)
        (io/on (fn [_]
                 (network @history)) result*)
        (io/on history (get-state size undo redo history network))
        result)))

(defmacro with-undo
  ;TODO make size and redo optional
  [size undo redo actions expr]
  (let [history (event/network)
        m (get-event-alias actions history)]
    `(with-undo* ~size ~undo ~redo ~history ~m ~(riddley/walk-exprs m m expr))))

(def switcher
  (comp m/join
        behavior/stepper))
