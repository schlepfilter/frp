(ns frp.browser
  (:require [aid.core :as aid]
            [frp.derived :as derived]
            [frp.primitives.behavior :as behavior]
            [frp.primitives.event :as event]))

(aid/defcurried effect
                [f x]
                (f x)
                x)

;TODO combine this function with redef-listen
(def redef-event
  #(behavior/redef %
                   (derived/event)))

(defn make-redef-event
  [e]
  #(redef-event e))

(def get-event
  (comp (effect (comp behavior/register*
                      make-redef-event))
        event/->Event))

#?(:clj (defmacro defevent
          [expr]
          `(def ~expr
             (get-event ~(->> expr
                              (str *ns* "/")
                              keyword)))))
