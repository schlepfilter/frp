(ns frp.derived
  (:require [cats.context :as ctx]
            [com.rpl.specter :as s]
            [help.core :as help]
            [frp.helpers :as helpers]
            [frp.clojure.core :as core]
            [frp.primitives.behavior :as behavior]
            [frp.primitives.event :as event]
            [frp.tuple :as tuple]
    #?(:clj
            [clojure.walk :as walk])))

(defn event
  ([]
   (->> (help/mempty)
        (ctx/with-context event/context)))
  ([a]
   (-> (event)
       help/infer
       (help/return a))))

(help/defcurried add-edges
                 [parents child network]
                 (helpers/call-functions (map ((help/flip event/add-edge)
                                                child)
                                              parents)
                                         network))

(defn get-occs-or-latests-coll
  [initial ids network]
  (map (partial (help/flip (event/make-get-occs-or-latests initial))
                network)
       ids))

(defn make-combine-occs-or-latests
  [f]
  (comp (help/build tuple/tuple
                    (comp tuple/fst first)
                    (comp (partial apply f)
                          (partial map tuple/snd)))
        vector))

(defn get-combined-occs
  [f parents initial network]
  (apply (partial map
                  (make-combine-occs-or-latests f))
         (get-occs-or-latests-coll initial
                                   parents
                                   network)))

(help/defcurried modify-combine
                 [f parents initial child network]
                 (event/set-occs (get-combined-occs f
                                                    parents
                                                    initial
                                                    network)
                                 child
                                 network))

(defn combine
  [f & parent-events]
  ((help/build (comp event/event*
                     cons)
               add-edges
               (comp event/make-set-modify-modify
                     (modify-combine f)))
    (map :id parent-events)))

(defn make-entity?
  [entity-type]
  (comp (partial = entity-type)
        type))

(def event?
  (make-entity? frp.primitives.event.Event))

(def behavior?
  (make-entity? frp.primitives.behavior.Behavior))

(defn behavior
  [a]
  (->> a
       help/pure
       (ctx/with-context behavior/context)))

(defn if-not-then-else
  [if-function then-function else]
  (helpers/if-then-else (complement if-function)
                        then-function
                        else))

(def behaviorize
  (partial if-not-then-else
           behavior?
           behavior))

(defn xor
  ;TODO support variadic arguments
  [p q]
  (or (and p (not q))
      (and (not p) q)))

(help/defcurried eventize
                 [e a]
                 ;TODO refactor
                 ;TODO use casep
                 (help/casep a
                             event? a
                             (help/<$> (constantly a)
                                       e)))

(def has-event?
  (partial some event?))

(defn entitize
  [arguments]
  (map (if (has-event? arguments)
         (->> arguments
              (filter event?)
              first
              eventize)
         behaviorize)
       arguments))

(def has-argument?
  (help/build and
              seq?
              (comp (partial not= 1)
                    count)))

#?(:clj
   (do (defmacro transparent*
         [[f & more]]
         `(let [arguments# [~@more]]
            (if (xor (has-event? arguments#)
                     (some behavior? arguments#))
              (apply (if (has-event? arguments#)
                       (partial combine ~f)
                       (help/lift-a ~f))
                     (entitize arguments#))
              (apply ~f arguments#))))

       (defmacro transparent
         [expr]
         (walk/postwalk (fn [x]
                          (help/casep x
                                      has-argument? `(transparent* ~x)
                                      x))
                        (macroexpand expr)))))

(def accum
  (partial core/reduce (help/flip help/funcall)))

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
                                 (helpers/if-then-else (comp (partial =
                                                                      size)
                                                             count)
                                                       rest
                                                       accumulation)))
                     (rest []))
        (combine vector (core/count e))
        (core/filter (fn [[n xs]]
                       (and (= (count xs) size)
                            (= (rem (- n size) start) 0))))
        ;this is harder to read.
        ;(core/filter (helpers/build and
        ;                            (comp (partial = size)
        ;                                  count
        ;                                  last)
        ;                            (comp (partial = 0)
        ;                                  (partial (helpers/flip rem) start)
        ;                                  (partial + size)
        ;                                  -
        ;                                  first)))
        (help/<$> second))))

(def mean
  (help/build (partial combine /)
              core/+
              core/count))

(def switcher
  (comp help/join
        behavior/stepper))
