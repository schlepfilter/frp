;primitives.event and io namespaces are separated to limit the impact of :refer-clojure :exclude for transduce
(ns frp.io
  (:require [aid.core :as aid]
            [com.rpl.specter :as s]
            [frp.primitives.behavior :as behavior]
            [frp.primitives.event :as event]
            [frp.protocols :as protocols]
            [frp.tuple :as tuple])
  #?(:cljs (:require-macros [frp.io :refer [defcurriedmethod]])))

(defmulti run-effect! (comp protocols/-get-keyword
                            second
                            vector))
;This definition of get-effect! produces the following failure in :advanced.
;Reloading Clojure file "/nodp/hfdp/observer/synchronization.clj" failed.
;clojure.lang.Compiler$CompilerException: java.lang.IllegalArgumentException: No method in multimethod 'get-effect!' for dispatch value
;(defmulti get-effect! (comp helpers/infer
;                            second
;                            vector))

(defmacro defcurriedmethod
  [multifn dispatch-val bindings & body]
  `(aid/defpfmethod ~multifn ~dispatch-val
                    (aid/curry ~(count bindings) (fn ~bindings
                                                   ~@body))))

(defcurriedmethod run-effect! :event
                  [f! e network]
                  (->> network
                       (event/get-latests (:id e))
                       (run! (comp f!
                                   tuple/snd)))
                  ((:network-id e) @event/universe-state))

(aid/defcurried get-network-value
  [b network]
  (behavior/get-value b (:time network) network))

(aid/defcurried set-cache
  [b network]
  (s/setval [:cache (:id b)] (get-network-value b network) network))

(defcurriedmethod
  run-effect! :behavior
  [f! b network]
  (aid/if-else (aid/build =
                          identity
                          (set-cache b))
               (comp f!
                     (get-network-value b))
               network)
  (set-cache b ((:network-id b) @event/universe-state)))

(defn on
  [f! x]
  (swap! event/universe-state
         (partial s/setval*
                  [(:network-id x) :effects s/AFTER-ELEM]
                  (run-effect! f! x))))
