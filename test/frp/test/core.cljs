(ns ^:figwheel-always frp.test.core
  (:require [doo.runner :refer-macros [doo-all-tests]]
            [frp.test.io]
            [frp.test.location]
            [frp.test.primitives.behavior]
            [frp.test.primitives.event]
            [frp.test.time]
            [frp.test.tuple]
            [frp.test.window]))

(enable-console-print!)

(doo-all-tests)
