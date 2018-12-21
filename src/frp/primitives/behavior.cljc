(ns frp.primitives.behavior
  (:refer-clojure :exclude [stepper time])
  (:require [clojure.set :as set]
            [aid.core :as aid]
            [cats.builtin]
            [cats.core :as m]
            [cats.protocols :as cats-protocols]
            [cats.util :as util]
            [com.rpl.specter :as s]
            [frp.primitives.event :as event]
            [frp.protocols :as entity-protocols]
            [frp.tuple :as tuple])
  #?(:clj (:import [clojure.lang IDeref])))

(declare context)

(defrecord Behavior
  ;TODO rename id as behavior-id
  [network-id id]
  cats-protocols/Contextual
  (-get-context [_]
    context)
  entity-protocols/Entity
  (-get-keyword [_]
    :behavior)
  IDeref
  (#?(:clj  deref
      :cljs -deref) [_]
    (->> @event/universe-state
         network-id
         ((m/<*> (comp id
                       :function)
                 :time))))
  cats-protocols/Printable
  (-repr [_]
    (str "#[behavior " network-id " " id "]")))

(util/make-printable Behavior)

(defn behavior**
  [network-id id f]
  (swap! event/universe-state (partial s/setval* [network-id :function id] f))
  (Behavior. network-id id))

(def behavior*
  #(behavior** event/*network-id*
               (->> @event/universe-state
                    event/*network-id*
                    (event/get-id :function))
               %))

(defn get-function
  [b network]
  (->> network
       :function
       ((:id b))))

(defn get-value
  [b t network]
  ((get-function b network) t))

(defn get-universe-value
  [b t universe]
  (->> universe
       ((:network-id b))
       (get-value b t)))

(def pure
  (comp behavior*
        constantly))

(defn join
  [b]
  (behavior* #(-> b
                  (get-universe-value % @event/universe-state)
                  (get-universe-value % @event/universe-state))))

;Calling ap in -fapply is visibly slower.
;(def context
;  (helpers/reify-monad (fn [f fa]
;                         (behavior* #(-> fa
;                                         (get-value % @event/network-state)
;                                         f)))
;                       pure
;                       (fn [f]
;                         (behavior* #(-> f
;                                         (get-value % @event/network-state)
;                                         (get-value % @event/network-state))))))
(def context
  (reify
    cats-protocols/Context
    cats-protocols/Functor
    (-fmap [_ f fa]
      (behavior* #(-> fa
                      (get-universe-value % @event/universe-state)
                      f)))
    cats-protocols/Applicative
    (-pure [_ v]
      (pure v))
    (-fapply [_ fab fa]
      (behavior* #((get-universe-value fab % @event/universe-state)
                    (get-universe-value fa % @event/universe-state))))
    cats-protocols/Monad
    (-mreturn [_ a]
      (pure a))
    (-mbind [_ ma f]
      (join (m/<$> f ma)))))

(def stop
  #((->> @event/universe-state
         :cancellations
         (apply juxt aid/nop))))

(aid/defcurried rename-id
  [network-id to from universe]
  (s/transform [network-id (->> [:dependency
                                 :function
                                 :modifications
                                 :modified
                                 :occs]
                                (map s/must)
                                (apply s/multi-path))]
               (partial (aid/flip set/rename-keys)
                        {from to})
               universe))

(def rename-id!
  (comp (partial swap! event/universe-state)
        rename-id))

(defn redef
  [to from]
  (rename-id! (:network-id to) (:id to) (:id from)))

(def time
  (Behavior. event/*network-id* ::time))

;TODO only use registry for debugging
(def registry
  (atom []))

(def register!
  (comp (partial swap! registry)
        ((aid/curry 3 s/setval*) s/AFTER-ELEM)))

(defn start
  []
  (reset! event/universe-state event/initial-universe)
  (redef time
         (behavior* identity))
  (run! aid/funcall @registry))

(def restart
  (juxt stop
        start))

(defn last-pred
  [default pred coll]
  (->> coll
       reverse
       (drop-while (complement pred))
       (take 1)
       (cons default)
       last))
;last-pred can be O(log(n))
;(defn get-middle
;  [left right]
;  (+ left (quot (- right left) 2)))
;
;(defn first-pred-index
;  [pred left right coll]
;  (if (= left right)
;    left
;    (if (->> (get-middle left right)
;             (get coll)
;             pred)
;      (recur pred left (get-middle left right) coll)
;      (recur pred (inc (get-middle left right)) right coll))))
;
;(defn last-pred
;  [default pred coll]
;  (nth coll
;       (dec (first-pred-index (complement pred) 0 (count coll) coll))
;       default))

(defn get-stepper-value
  [a e t universe]
  (->> universe
       ((:network-id e))
       (event/get-occs (:id e))
       (last-pred (event/get-unit a) (comp (partial > @t)
                                           deref
                                           tuple/fst))
       tuple/snd))

(defn stepper
  [a e]
  (behavior* #(get-stepper-value a e % @event/universe-state)))

(defn get-time-transform-function
  ;TODO refactor
  [any-behavior time-behavior network]
  (comp (get-function any-behavior network)
        (get-function time-behavior network)))

(defn time-transform
  ;TODO throw an error if any-behavior is created directly or indirectly by stepper
  ;TODO refactor
  [any-behavior time-behavior]
  (behavior* (get-time-transform-function any-behavior
                                          time-behavior
                                          @event/universe-state)))

;TODO implement calculus after a Clojure/ClojureScript library for symbolic computation is released
