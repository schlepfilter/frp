(ns frp.time
  (:refer-clojure :exclude [time])
  (:require [aid.core :as aid]
            [cats.protocols :as p]
            [cats.util :as util]
    #?@(:clj  [
            [clj-time.coerce :as c]
            [clj-time.core :as t]]
        :cljs [[cljs-time.coerce :as c]
               [cljs-time.core :as t]]))
  #?(:clj
     (:import (clojure.lang IDeref))))

(defrecord Time
  [x]
  p/Contextual
  (-get-context [_]
    (reify
      p/Context
      p/Semigroup
      (-mappend [_ x* y*]
        (Time. (max @x* @y*)))
      p/Monoid
      (-mempty [_]
        (Time. 0))
      p/Functor
      (-fmap [_ f fa]
        (-> @fa
            f
            Time.))))
  IDeref
  (#?(:clj  deref
      :cljs -deref) [_]
    x)
  #?(:clj  Comparable
     :cljs IComparable)
  (#?(:clj  compareTo
      :cljs -compare) [x* y*]
    (compare @x* @y*))
  p/Printable
  (-repr [_]
    (str "#[time " x "]")))

(util/make-printable Time)

(def time
  ->Time)

(def epoch-state
  (atom 0))

(defn now-long
  []
  (-> (t/now)
      c/to-long))

(defn start
  []
  (->> (now-long)
       ;dec ensures times for events are strictly increasing.
       dec
       (reset! epoch-state)))

(defn now
  []
  (-> (now-long)
      (- @epoch-state)
      time))

(defn to-real-time
  [t]
  (aid/<$> (partial + @epoch-state)
           t))

(def epoch
  (time 0))
