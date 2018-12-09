(ns frp.clojure.core
  (:refer-clojure :exclude [+
                            count
                            drop
                            filter
                            max
                            merge-with
                            min
                            reduce
                            remove])
  (:require [clojure.core :as core]
            [aid.unit :as unit]
            [cats.core :as m]
            [frp.primitives.event :as event]))

(defn reduce
  ([f e]
   (->> e
        (event/transduce (core/drop 0)
                         (fn [{:keys [event-value start]} element]
                           {:event-value (if start
                                           element
                                           (f event-value element))
                            :start       false})
                         {:event-value unit/unit
                          :start       true})
        (m/<$> :event-value)))
  ([f val e]
   (event/transduce (core/drop 0) f val e)))

(defn filter
  [pred e]
  (event/transduce (core/filter pred)
                   (comp second
                         vector)
                   unit/unit
                   e))

(defn remove
  [pred e]
  (filter (complement pred) e))

(def max
  (partial reduce core/max #?(:clj  Double/NEGATIVE_INFINITY
                              :cljs js/Number.NEGATIVE_INFINITY)))

(def min
  (partial reduce core/min #?(:clj  Double/POSITIVE_INFINITY
                              :cljs js/Number.POSITIVE_INFINITY)))

(def +
  (partial reduce core/+))

(def count
  (partial event/transduce (map (constantly 1)) core/+))

(defn drop
  [n e]
  (event/transduce (core/drop n)
                   (comp second
                         vector)
                   e))

(defn merge-with
  [f e]
  (reduce (partial core/merge-with f) e))
