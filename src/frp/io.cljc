;primitives.event and io namespaces are separated to limit the impact of :refer-clojure :exclude for transduce
(ns frp.io
  (:require [aid.core :as aid]
            [com.rpl.specter :as s]
            [frp.primitives.behavior :as behavior]
            [frp.primitives.event :as event]
            [frp.primitives.net :as net]
            [frp.tuple :as tuple]))

(aid/defcurried run-event-effect!
  [f! e net]
  (->> net
       (event/get-latests (:entity-id e))
       (run! (comp f!
                   tuple/snd))))

(aid/defcurried get-net-value
  [b net]
  (behavior/get-value b (:time net) net))

(defn memoize-one
  [f!]
  ;TODO use core.memoize when core.memoize supports ClojureScript
  (let [state (atom {})]
    (fn [& more]
      (aid/case-eval more
        (:arguments @state) (:return @state)
        (->> more
             (apply f!)
             (event/effect #(reset! state {:arguments more
                                           :return    %})))))))

(aid/defcurried run-behavior-effect!
  [f! b net]
  (->> net
       (get-net-value b)
       f!))

(defn run*
  [effect-id f! x]
  (swap! net/universe-state
         (partial s/setval*
                  [(:net-id x) :effect effect-id]
                  ((aid/casep x
                     event/event? (run-event-effect! f!)
                     (run-behavior-effect! (memoize-one f!)))
                    x))))

(defn run
  [f! x]
  (run* (->> @net/universe-state
             ((:net-id x))
             :effect
             net/get-id)
        f!
        x))
