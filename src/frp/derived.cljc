(ns frp.derived
  (:require [clojure.walk :as walk]
            [aid.core :as aid]
            [cats.core :as m]
            [frp.clojure.core :as core]
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

(def switcher
  (comp m/join
        behavior/stepper))
