;primitives.event and io namespaces are separated to limit the impact of :refer-clojure :exclude for transduce
(ns frp.io
  (:require [aid.core :as aid :include-macros true]
            [cats.monad.maybe :as maybe]
            [com.rpl.specter :as s :include-macros true]
            [frp.derived :as derived]
            [frp.primitives.behavior :as behavior]
            [frp.primitives.event :as event]
            [frp.protocols :as protocols]
            [frp.tuple :as tuple])
  #?(:cljs (:require-macros [frp.io :refer [defcurriedmethod]])))

(defmulti get-effect! (comp protocols/-get-keyword
                            second
                            vector))

;This definition of get-effect! produces the following failure in :advanced.
;Reloading Clojure file "/nodp/hfdp/observer/synchronization.clj" failed.
;clojure.lang.Compiler$CompilerException: java.lang.IllegalArgumentException: No method in multimethod 'get-effect!' for dispatch value
;(defmulti get-effect! (comp helpers/infer
;                            second
;                            vector))

#?(:clj (defmacro defcurriedmethod
          [multifn dispatch-val bindings & body]
          `(aid/defpfmethod ~multifn ~dispatch-val
                            (aid/curry ~(count bindings)
                                       (fn ~bindings
                                         ~@body)))))

(defcurriedmethod get-effect! :event
                  [f! e network]
                  (run! (comp f!
                              tuple/snd)
                        (event/get-latests (:id e) network))
                  network)

(defn get-network-value
  [b network]
  (behavior/get-value b (:time network) network))

(defcurriedmethod get-effect! :behavior
                  [f! b network]
                  ;TODO don't use maybe
                  (if (= (maybe/just (get-network-value b network))
                         ((:id b) (:cache network)))
                    network
                    (do (f! (get-network-value b network))
                        (s/setval [:cache (:id b)]
                                  (maybe/just (get-network-value b network))
                                  network))))

(def on
  (comp (partial swap! event/network-state)
        ((aid/curry 3 s/setval*) [:effects s/END])
        vector
        get-effect!))

(def redef-events
  (partial run! (fn [from]
                  (behavior/redef from
                                  (derived/event)))))
