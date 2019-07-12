(ns frp.helpers
  (:refer-clojure :exclude [<=]))

(def <=
  (comp (partial every? (comp not
                              pos?
                              (partial apply compare)))
        (partial partition 2 1)
        vector))
