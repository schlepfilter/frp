(ns frp.browser
  (:require [aid.core :as aid]
            [com.rpl.specter :as s]
            [frp.derived :as derived]
            [frp.primitives.behavior :as behavior]
            [frp.primitives.event :as event]))

(aid/defcurried effect
                [f x]
                (f x)
                x)

(defn make-redef-event
  [e]
  #(behavior/redef e
                   (derived/event)))

(def get-event
  (comp (effect (comp behavior/register*
                      make-redef-event))
        event/->Event))

(defn add-remove-listener
  [event-type listener]
  #?(:cljs
     (do (js/addEventListener event-type listener)
         (swap! event/network-state
                (partial s/setval*
                         :cancel
                         (fn [_]
                           (js/removeEventListener event-type listener)))))))

(defn listen
  [f e]
  (-> e
      :id
      name
      (add-remove-listener (comp e
                                 f))))

(def get-caller-keyword
  #(->> %
        (str *ns* "/")
        keyword))

#?(:clj (defmacro defevent
          ([expr]
           `(def ~expr
              (get-event ~(get-caller-keyword expr))))
          ([expr f]
           `(listen ~f (defevent ~expr)))))
