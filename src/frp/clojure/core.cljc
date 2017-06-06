(ns frp.clojure.core
  (:refer-clojure :exclude [+ count drop filter max min reduce remove])
  (:require [aid.core :as aid]
            [aid.unit :as unit]
            [frp.primitives.event :as event]))

(defn reduce
  ([f e]
   (->> e
        (event/transduce (clojure.core/drop 0)
                         (fn [{:keys [event-value start]} element]
                           {:event-value (if start
                                           element
                                           (f event-value element))
                            :start       false})
                         {:event-value unit/unit
                          :start       true})
        (aid/<$> :event-value)))
  ([f val e]
   (event/transduce (clojure.core/drop 0) f val e)))

(defn filter
  [pred e]
  (event/transduce (clojure.core/filter pred)
                   (comp second
                         vector)
                   unit/unit
                   e))

(defn remove
  [pred e]
  (filter (complement pred) e))

(def max
  (partial reduce clojure.core/max #?(:clj  Double/NEGATIVE_INFINITY
                                      :cljs js/Number.NEGATIVE_INFINITY)))

(def min
  (partial reduce clojure.core/min #?(:clj  Double/POSITIVE_INFINITY
                                      :cljs js/Number.POSITIVE_INFINITY)))

(def +
  (partial reduce clojure.core/+ 0))

(def count
  (partial event/transduce (map (constantly 1)) clojure.core/+ 0))

(defn drop
  [n e]
  (event/transduce (clojure.core/drop n)
                   (comp second
                         vector)
                   e))
