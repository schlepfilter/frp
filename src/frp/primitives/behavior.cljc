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
            [frp.primitives.net :as net]
            [frp.protocols :as entity-protocols]
            [frp.tuple :as tuple])
  #?(:clj (:import [clojure.lang IDeref])))

(declare get-context)

(defrecord Behavior
  ;TODO rename id as behavior-id
  [net-id entity-id]
  cats-protocols/Contextual
  (-get-context [_]
    (get-context net-id))
  entity-protocols/Entity
  (-get-keyword [_]
    :behavior)
  IDeref
  (#?(:clj  deref
      :cljs -deref) [_]
    (->> @net/universe-state
         net-id
         ((m/<*> (comp entity-id
                       :function)
                 :time))))
  cats-protocols/Printable
  (-repr [_]
    (str "#[behavior " net-id " " entity-id "]")))

(util/make-printable Behavior)

(defn behavior**
  [net-id entity-id f]
  (swap! net/universe-state (partial s/setval* [net-id
                                                :function
                                                entity-id]
                                     f))
  (Behavior. net-id entity-id))

(aid/defcurried behavior*
  [net-id f]
  (behavior** net-id
              (->> @net/universe-state
                   net-id
                   :function
                   net/get-id)
              f))

(defn get-function
  [b net]
  (->> net
       :function
       ((:entity-id b))))

(defn get-value
  [b t net]
  ((get-function b net) t))

(defn get-universe-value
  [b t universe]
  (->> universe
       ((:net-id b))
       (get-value b t)))

(defn pure*
  [net-id f]
  (->> f
       constantly
       (behavior* net-id)))

(def pure
  #(pure* net/*net-id* %))

(defn join
  [b]
  (behavior* (:net-id b)
             #(-> b
                  (get-value % ((:net-id b) @net/universe-state))
                  (get-value % ((:net-id b) @net/universe-state)))))

;Calling ap in -fapply is visibly slower.
;(def context
;  (helpers/reify-monad (fn [f fa]
;                         (behavior* #(-> fa
;                                         (get-value % @event/net-state)
;                                         f)))
;                       pure
;                       (fn [f]
;                         (behavior* #(-> f
;                                         (get-value % @event/net-state)
;                                         (get-value % @event/net-state))))))
(defn get-context
  [net-id]
  (reify
    entity-protocols/Entity
    (-get-net-id [_]
      net-id)
    cats-protocols/Context
    cats-protocols/Functor
    (-fmap [_ f! fa]
      (behavior* (:net-id fa)
                 #(-> fa
                      (get-universe-value % @net/universe-state)
                      f!)))
    cats-protocols/Applicative
    (-pure [context* a]
      (pure* (entity-protocols/-get-net-id context*) a))
    (-fapply [_ fab fa]
      (behavior* (:net-id fab)
                 #((get-universe-value fab % @net/universe-state)
                    (get-universe-value fa % @net/universe-state))))
    cats-protocols/Monad
    (-mreturn [context* a]
      (cats-protocols/-pure context* a))
    (-mbind [_ ma f!]
      (join (m/<$> f! ma)))))

(def stop
  #((->> @net/universe-state
         vals
         :cancellations
         (apply juxt aid/nop))))

(aid/defcurried rename-id
  [to from universe]
  (s/transform [(:net-id to) (->> (aid/casep to
                                    event/event? [:dependency
                                                  :modifications
                                                  :modified
                                                  :occs]
                                    [:function])
                                  (map s/must)
                                  (apply s/multi-path))]
               (partial (aid/flip set/rename-keys)
                        (apply hash-map (map :entity-id [from to])))
               universe))

(def redef
  (comp (partial swap! net/universe-state)
        rename-id))

(def time
  (Behavior. net/initial-net-id ::time))

;TODO only use registry for debugging
(def registry
  (atom []))

(def register!
  (comp (partial swap! registry)
        ((aid/curry 3 s/setval*) s/AFTER-ELEM)))

(defn start
  []
  (reset! net/universe-state net/initial-universe)
  (redef time
         (behavior* net/initial-net-id identity))
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
       ((:net-id e))
       (event/get-occs (:entity-id e))
       (last-pred (event/get-unit a) (comp (partial > @t)
                                           deref
                                           tuple/fst))
       tuple/snd))

(defn stepper
  [a e]
  (behavior* (:net-id e)
             #(get-stepper-value a e % @net/universe-state)))

(restart)
