(ns frp.derived
  (:require [aid.core :as aid :include-macros true]
            [cats.context :as ctx]
            [cats.core :as m]
            [com.rpl.specter :as s :include-macros true]
            [frp.clojure.core :as core]
            [frp.primitives.behavior :as behavior]
            [frp.primitives.event :as event]
            [frp.tuple :as tuple]
            #?(:clj [clojure.walk :as walk])))

(defn event
  ([]
   (event/mempty))
  ([a]
   (event/pure a)))

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

(def event-only?
  (aid/build and
             (partial some event/event?)
             (complement (partial some behavior?))))

(def behavior-only?
  (aid/build and
             (partial some behavior?)
             (complement (partial some event/event?))))

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

#?(:clj
   (defmacro transparent
     [expr]
     (->> expr
          ;TODO macroexpand expr when ClojureScript starts supporting runtime macro expansion
          ;macroexpand is only intended as a REPL utility
          ;https://cljs.github.io/api/cljs.core/macroexpand
          walk/macroexpand-all
          (walk/postwalk #(aid/casep %
                                     has-argument? `(apply transparent*
                                                           ~(vec %))
                                     %)))))

(def accum
  (partial core/reduce (aid/flip aid/funcall)))

(defn buffer
  ;TODO accept different types of arguments like http://reactivex.io/documentation/operators/buffer.html
  ;https://github.com/Reactive-Extensions/RxJS/blob/master/doc/api/core/operators/bufferwithcount.md
  ([size e]
   (buffer size size e))
  ([size skip e]
   (->> e
        (core/reduce (fn [reduction element]
                       (->> reduction
                            (aid/if-then (comp zero?
                                               (partial (aid/flip mod) skip)
                                               :start)
                                         (partial s/setval*
                                                  [:occs
                                                   s/AFTER-ELEM]
                                                  []))
                            (s/setval [:occs s/ALL s/AFTER-ELEM] element)
                            (s/transform :occs
                                         (partial remove (comp (partial < size)
                                                               count)))
                            (s/transform :start inc)))
                     {:occs  []
                      :start 0})
        (m/<$> (comp first
                     :occs))
        (core/filter (comp (partial = size)
                           count)))))

(def mean
  (comp (m/<$> (partial apply /))
        (partial core/reduce
                 (fn [reduction element]
                   (->> reduction
                        (s/transform s/FIRST (partial + element))
                        (s/transform s/LAST inc)))
                 [0 0])))

(def switcher
  (comp m/join
        behavior/stepper))
