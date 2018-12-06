(ns frp.test.doo
  (:require [doo.runner :refer-macros [doo-all-tests]]))

(doo-all-tests #"frp\.+")
