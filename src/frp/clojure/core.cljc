(ns frp.clojure.core
  (:refer-clojure :exclude [+
                            count
                            dedupe
                            distinct
                            drop
                            filter
                            group-by
                            max
                            merge
                            merge-with
                            min
                            partition
                            reduce
                            remove
                            vector])
  (:require [clojure.core :as core]
            [aid.core :as aid]
            [aid.unit :as unit]
            [cats.core :as m]
            [com.rpl.specter :as s]
            [frp.primitives.event :as event]
            [frp.primitives.net :as net]))

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
  ([f x e]
   (m/<> (net/with-net e
                       (event/pure x))
         (event/transduce (core/drop 0) f x e))))

(def reduce*
  (comp second
        core/vector))

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
            (update reduction
                    (f element)
                    (partial s/setval* s/AFTER-ELEM element)))
          {}
          e))

(def distinct
  (partial event/transduce (core/distinct) reduce*))

(defn partition
  ([n e]
   (partition n n e))
  ([n step e]
   (->> e
        (reduce (fn [reduction element]
                  (->> reduction
                       (aid/if-then (comp zero?
                                          (partial (aid/flip mod) step)
                                          :start)
                                    (partial s/setval*
                                             [:occs
                                              s/AFTER-ELEM]
                                             []))
                       (s/setval [:occs s/ALL s/AFTER-ELEM] element)
                       (s/transform :occs
                                    (partial core/remove (comp (partial < n)
                                                               core/count)))
                       (s/transform :start inc)))
                {:occs  []
                 :start 0})
        (m/<$> (comp first
                     :occs))
        (filter (comp (partial = n)
                      core/count)))))

(def vector
  (partial reduce core/conj []))

(def dedupe
  (partial event/transduce (core/dedupe) reduce*))

(def merge
  (partial reduce core/merge))

