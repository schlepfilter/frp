(ns ^:figwheel-always frp.test.core
  (:require [cljs.test :refer-macros [run-all-tests]]
            [frp.test.io]
            [frp.test.location]
            [frp.test.primitives.behavior]
            [frp.test.primitives.event]
            [frp.test.time]
            [frp.test.tuple]
            [frp.test.window]))

(enable-console-print!)

(run-all-tests)
