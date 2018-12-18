(ns frp.incanter.distributions
  (:require [cats.core :as m]
            [com.rpl.specter :as s]
            [frp.clojure.core :as core]))

(def mean
  (comp (m/<$> (partial apply /))
        (partial core/reduce
                 (fn [reduction element]
                   (->> reduction
                        (s/transform s/FIRST (partial + element))
                        (s/transform s/LAST inc)))
                 [0 0])))
