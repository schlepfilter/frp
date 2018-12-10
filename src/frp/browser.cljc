(ns frp.browser
  (:require [frp.derived :as derived]
            [frp.primitives.behavior :as behavior]
            [frp.primitives.event :as event]))

(def redef-event
  #(behavior/redef % (derived/event)))

(defn get-event
  [k]
  (let [e (event/->Event k)]
    (-> e
        redef-event
        behavior/register)
    e))

#?(:clj (defmacro defevent
          [expr]
          `(def ~expr
             (get-event ~(->> expr
                              (str *ns* "/")
                              keyword)))))
