(ns frp.test.doo
  (:require [doo.runner :refer-macros [doo-all-tests]]
            [frp.test.core]))

(doo-all-tests #"frp.+")
