;primitives.event and io namespaces are separated to limit the impact of :refer-clojure :exclude for transduce
(ns frp.io
  (:require [aid.core :as aid]
            [com.rpl.specter :as s]
            [frp.primitives.behavior :as behavior]
            [frp.primitives.event :as event]
            [frp.tuple :as tuple]))

(aid/defcurried run-event-effect!
  [f! e network]
  (->> network
       (event/get-latests (:id e))
       (run! (comp f!
                   tuple/snd)))
  ;TODO extract a function
  ((:network-id e) @event/universe-state))

(aid/defcurried get-network-value
  [b network]
  (behavior/get-value b (:time network) network))

(aid/defcurried set-cache
  [effect-id b network]
  (s/setval [:cache effect-id] (get-network-value b network) network))

(aid/defcurried run-behavior-effect!
  [effect-id f! b network]
  (aid/if-else (aid/build =
                          identity
                          (set-cache effect-id b))
               (comp f!
                     (get-network-value b))
               network)
  (set-cache effect-id b ((:network-id b) @event/universe-state)))

(defn on
  [f! x]
  (let [effect-id (->> @event/universe-state
                       ((:network-id x))
                       :effect
                       event/get-id)]
    (swap! event/universe-state
           (partial s/setval*
                    [(:network-id x) :effect effect-id]
                    ((aid/casep x
                       event/event? run-event-effect!
                       (run-behavior-effect! effect-id))
                      f! x)))))
