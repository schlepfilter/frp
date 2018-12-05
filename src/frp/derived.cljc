(ns frp.derived
  (:require [aid.core :as aid :include-macros true]
            [cats.context :as ctx]
            [cats.core :as m]
            [com.rpl.specter :as s :include-macros true]
            [frp.helpers :as helpers :include-macros true]
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

(aid/defcurried add-edges
                [parents child network]
                (helpers/call-functions (map ((aid/flip event/add-edge) child)
                                             parents)
                                        network))

(defn get-occs-or-latests-coll
  [initial ids network]
  (map (partial (aid/flip (event/make-get-occs-or-latests initial)) network)
       ids))

(defn make-combine-occs-or-latests
  [f]
  (comp (aid/build tuple/tuple
                   (comp tuple/fst first)
                   (comp (partial apply f)
                         (partial map tuple/snd)))
        vector))

(defn get-combined-occs
  [f parents initial network]
  (apply (partial map (make-combine-occs-or-latests f))
         (get-occs-or-latests-coll initial parents network)))

(aid/defcurried modify-combine
                [f parents initial child network]
                (event/set-occs (get-combined-occs f parents initial network)
                                child
                                network))

(defn combine
  [f & parent-events]
  ((aid/build (comp event/event*
                    cons)
              add-edges
              (comp event/make-set-modify-modify
                    (modify-combine f)))
    (map :id parent-events)))

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
  ([size start e]
   (->> e
        (core/reduce (fn [accumulation element]
                       (s/setval s/END
                                 [element]
                                 (aid/if-then (comp (partial = size)
                                                    count)
                                              rest
                                              accumulation)))
                     (rest []))
        (combine vector (core/count e))
        (core/filter (fn [[n xs]]
                       (and (= (count xs) size)
                            (= (rem (- n size) start) 0))))
        ;This is harder to read.
        ;(core/filter (aid/build and
        ;                        (comp (partial = size)
        ;                              count
        ;                              last)
        ;                        (comp (partial = 0)
        ;                              (partial (aid/flip rem) start)
        ;                              (partial + size)
        ;                              -
        ;                              first)))
        (m/<$> second))))

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
