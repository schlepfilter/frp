(ns frp.browser
  (:require [frp.derived :as derived]
            [frp.primitives.behavior :as behavior]
            [frp.primitives.event :as event]))

(def redef-event
  #(behavior/redef % (derived/event)))

#?(:clj (defmacro defevent
          [expr]
          `(def ~expr
             (let [e# (event/->Event ~(keyword (str *ns* "/" expr)))]
               (behavior/register (redef-event e#))
               e#))))
