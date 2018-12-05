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

(def has-argument?
  (aid/build and
             seq?
             (comp (partial not= 1)
                   count)))

#?(:clj
   (do (defmacro transparent*
         [[f & more]]
         `(let [arguments# [~@more]]
            (if (some behavior? arguments#)
              (->> arguments#
                   (map behaviorize)
                   (apply (aid/lift-a ~f)))
              (apply ~f arguments#))))

       (defmacro transparent
         [expr]
         ;TODO use if-then
         (walk/postwalk (fn [x]
                          (aid/casep x
                                     has-argument? `(transparent* ~x)
                                     x))
                        (macroexpand expr)))))

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
