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
    (-> @event/universe-state
        network-id
        ((aid/build aid/funcall
                    (comp id
                          :function)
                    identity
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
                    :function
                    event/get-id)
               %))

(defn get-function
  [b network]
  (->> network
       :function
       ((:id b))))

(aid/defcurried get-value
  [b t network]
  ((get-function b network) network t))

(defn get-universe-value
  [b t universe]
  (->> universe
       ((:network-id b))
       (get-value b t)))

(def pure
  (comp behavior*
        constantly))

(defn join*
  [b network t]
  (-> b
      (get-value t network)
      (get-value t network)))

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
    (-fmap [_ f! fa]
      (behavior* (fn [network t]
                   (->> network
                        (get-value fa t)
                        f!))))
    cats-protocols/Applicative
    (-pure [_ v]
      (pure v))
    (-fapply [_ fab fa]
      (behavior* (fn [network t]
                   ((m/<*> (get-value fab t)
                           (get-value fa t))
                     network))))
    cats-protocols/Monad
    (-mreturn [_ a]
      (pure a))
    (-mbind [_ ma f!]
      (behavior* (fn [_ t]
                   (join* (m/<$> f! ma) ((:network-id ma) @event/universe-state)
                          t))))))

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
         (behavior* (fn [_ t]
                      t)))
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

(defn stepper
  [a e]
  (behavior* (fn [network t]
               (->> network
                    (event/get-occs (:id e))
                    (last-pred (event/get-unit a) (comp (partial > @t)
                                                        deref
                                                        tuple/fst))
                    tuple/snd))))
