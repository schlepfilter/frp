(ns frp.test.time
  (:require [clojure.test.check]
            [clojure.test.check.clojure-test :as clojure-test :include-macros true]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop :include-macros true]
            [frp.time :as time]
            [frp.test.helpers :as helpers]))

(clojure-test/defspec
  time-increasing
  helpers/cljc-num-tests
  (prop/for-all []
                (<= (time/now-long)
                    @(time/to-real-time (time/now))
                    (time/now-long))))
