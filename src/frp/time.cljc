(ns frp.time
  (:refer-clojure :exclude [time])
  (:require [cats.core :as m]
            [cats.protocols :as p]
            [cats.util :as util]
            #?@(:clj  [[clj-time.coerce :as c]
                       [clj-time.core :as t]]
                :cljs [[cljs-time.coerce :as c]
                       [cljs-time.core :as t]]))
  #?(:clj (:import (clojure.lang IDeref))))

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

(def real-epoch-state
  (atom 0))

(def now-long
  #(-> (t/now)
       c/to-long))

(def start
  #(->> (now-long)
        ;dec ensures times for events are strictly increasing.
        dec
        (reset! real-epoch-state)))

(def now
  #(-> (now-long)
       (- @real-epoch-state)
       time))

(def to-real-time
  #(m/<$> (partial + @real-epoch-state) %))

(def epoch
  (time 0))
