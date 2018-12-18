(ns frp.clojure.core
  (:refer-clojure :exclude [+
                            count
                            distinct
                            drop
                            filter
                            group-by
                            max
                            merge-with
                            min
                            reduce
                            remove])
  (:require [clojure.core :as core]
            [aid.unit :as unit]
            [cats.core :as m]
            [com.rpl.specter :as s]
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

(def reduce*
  (comp second
        vector))

(defn filter
  [pred e]
  (event/transduce (core/filter pred) reduce* unit/unit e))

(defn remove
  [pred e]
  (filter (complement pred) e))

(def max
  (partial reduce core/max #?(:clj  Double/NEGATIVE_INFINITY
                              :cljs js/Number.NEGATIVE_INFINITY)))

(def min
  (partial reduce core/min event/positive-infinity))

(def +
  (partial reduce core/+))

(def count
  (partial event/transduce (map (constantly 1)) core/+))

(defn drop
  [n e]
  (event/transduce (core/drop n) reduce* e))

(defn merge-with
  [f e]
  (reduce (partial core/merge-with f) e))

(defn group-by
  [f e]
  (reduce (fn [reduction element]
            (s/setval [(f element) s/AFTER-ELEM] element reduction))
          {}
          e))

(def distinct
  (partial event/transduce (core/distinct) reduce*))
