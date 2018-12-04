(ns frp.test.time
  (:require [clojure.test.check]
            [clojure.test.check.clojure-test :as clojure-test :include-macros true]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop :include-macros true]
            [frp.helpers :as helpers]
            [frp.time :as time]
            [frp.test.helpers :as test-helpers]))

(clojure-test/defspec
  time-increasing
  test-helpers/cljc-num-tests
  (prop/for-all []
    (helpers/<= (time/now)
                (time/now))))
