(ns frp.primitives.behavior
  (:refer-clojure :exclude [stepper time])
  (:require [clojure.set :as set]
            [aid.core :as aid]
            [cats.builtin]
            [cats.core :as m]
            [cats.protocols :as protocols]
            [cats.util :as util]
            [com.rpl.specter :as s]
            [frp.helpers :as helpers :include-macros true]
            [frp.primitives.event :as event]
            [frp.protocols :as entity-protocols]
            [frp.tuple :as tuple])
  #?(:clj (:import [clojure.lang IDeref])))

(declare context)

(defrecord Behavior
  [id]
  protocols/Contextual
  (-get-context [_]
    context)
  entity-protocols/Entity
  (-get-keyword [_]
    :behavior)
  IDeref
  (#?(:clj  deref
      :cljs -deref) [_]
    ((m/<*> (comp id
                  :function)
            :time)
      @event/network-state))
  protocols/Printable
  (-repr [_]
    (str "#[behavior " id "]")))

(util/make-printable Behavior)

(defn behavior**
  [id f]
  (swap! event/network-state (partial s/setval* [:function id] f))
  (Behavior. id))

(defn behavior*
  [f]
  (-> @event/network-state
      event/get-id
      (behavior** f)))

(defn get-function
  [b network]
  ((:id b) (:function network)))

(defn get-value
  [b t network]
  ((get-function b network) t))

(def pure
  (comp behavior*
        constantly))

(def context
  (helpers/reify-monad
    (fn [f fa]
      (behavior* (fn [t]
                   (-> fa
                       (get-value t @event/network-state)
                       f))))
    pure
    #(behavior* (fn [t]
                  (-> %
                      (get-value t @event/network-state)
                      (get-value t @event/network-state))))))

(defn stop
  []
  ((:cancel @event/network-state)))

(def rename-id
  (comp ((aid/curry 3 s/transform*)
          (apply s/multi-path
                 (map s/must
                      [:dependency :function :modifies! :modified :occs])))
        (aid/flip (aid/curry 2 set/rename-keys))
        (partial apply array-map)
        reverse
        vector))

(def rename-id!
  (comp (partial swap! event/network-state)
        rename-id))

(defn redef
  [to from]
  (rename-id! (:id to) (:id from)))

(def time
  (Behavior. ::time))

(def registry
  (atom []))

(def register!
  (comp (partial swap! registry)
        ((aid/curry 3 s/setval*) s/END)
        vector))

(defn start
  []
  (reset! event/network-state (event/get-initial-network))
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
  [a e t network]
  (->> network
       (event/get-occs (:id e))
       (last-pred (event/get-unit a) (comp (partial > @t)
                                           deref
                                           tuple/fst))
       tuple/snd))

(defn stepper
  [a e]
  (behavior* (fn [t]
               (get-stepper-value a e t @event/network-state))))

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
                                          @event/network-state)))

;TODO implement calculus after a Clojure/ClojureScript library for symbolic computation is released
