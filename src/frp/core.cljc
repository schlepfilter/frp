(ns frp.core
  (:refer-clojure :exclude [stepper time transduce])
  (:require [frp.derived :as derived :include-macros true]
            [frp.io :as io]
            [frp.primitives.behavior :as behavior]
            [frp.primitives.event :as event]
    ;TODO don't require browser namespaces
            #?@(:cljs [[frp.document]
                       [frp.location]])))

(def restart
  behavior/restart)

(def event
  derived/event)

(def behavior
  derived/behavior)

(def time
  behavior/time)

(def stepper
  behavior/stepper)

(def time-transform
  behavior/time-transform)

(def transduce
  event/transduce)

(def snapshot
  event/snapshot)

(defmacro activate
  ([]
   `(event/activate))
  ([rate]
   `(event/activate ~rate)))

(def on
  io/on)

(defmacro transparent
  [expr]
  `(derived/transparent ~expr))

(def accum
  derived/accum)

(def buffer
  derived/buffer)

(def switcher
  derived/switcher)

;TODO move this expression to behavior
(restart)
