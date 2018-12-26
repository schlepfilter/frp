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
                   tuple/snd)))
  ;TODO extract a function
  ((:net-id e) @net/universe-state))

(aid/defcurried get-net-value
  [b net]
  (behavior/get-value b (:time net) net))

(aid/defcurried set-cache
  [effect-id b net]
  (s/setval [:cache effect-id] (get-net-value b net) net))

(aid/defcurried run-behavior-effect!
  [effect-id f! b net]
  (aid/if-else (aid/build =
                          identity
                          (set-cache effect-id b))
               (comp f!
                     (get-net-value b))
               net)
  (set-cache effect-id b ((:net-id b) @net/universe-state)))

(defn on*
  [effect-id f! x]
  (swap! net/universe-state
         (partial s/setval*
                  [(:net-id x) :effect effect-id]
                  ((aid/casep x
                     event/event? run-event-effect!
                     (run-behavior-effect! effect-id))
                    f! x))))

(defn on
  [f! x]
  (on* (->> @net/universe-state
            ((:net-id x))
            :effect
            net/get-id)
       f!
       x))
