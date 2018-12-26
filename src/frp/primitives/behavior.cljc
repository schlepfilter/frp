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

(declare get-context)

(defrecord Behavior
  ;TODO rename id as behavior-id
  [network-id id]
  cats-protocols/Contextual
  (-get-context [_]
    (get-context network-id))
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

(aid/defcurried behavior*
  [network-id f]
  (behavior** network-id
              (->> @event/universe-state
                   network-id
                   :function
                   event/get-id)
              f))

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

(defn pure*
  [network-id f]
  (->> f
       constantly
       (behavior* network-id)))

(def pure
  #(pure* event/*network-id* %))

(defn join
  [b]
  (behavior* (:network-id b)
             #(-> b
                  (get-value % ((:network-id b) @event/universe-state))
                  (get-value % ((:network-id b) @event/universe-state)))))

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
(defn get-context
  [network-id]
  (reify
    entity-protocols/Entity
    (-get-network-id [_]
      network-id)
    cats-protocols/Context
    cats-protocols/Functor
    (-fmap [_ f! fa]
      (behavior* (:network-id fa)
                 #(-> fa
                      (get-universe-value % @event/universe-state)
                      f!)))
    cats-protocols/Applicative
    (-pure [context* a]
      (pure* (entity-protocols/-get-network-id context*) a))
    (-fapply [_ fab fa]
      (behavior* (:network-id fab)
                 #((get-universe-value fab % @event/universe-state)
                    (get-universe-value fa % @event/universe-state))))
    cats-protocols/Monad
    (-mreturn [context* a]
      (cats-protocols/-pure context* a))
    (-mbind [_ ma f!]
      (join (m/<$> f! ma)))))

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
  (Behavior. event/initial-network-id ::time))

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
         (behavior* event/initial-network-id identity))
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
  (behavior* (:network-id e)
             #(get-stepper-value a e % @event/universe-state)))
