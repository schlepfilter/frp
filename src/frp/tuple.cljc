;TODO use cats.data when pair implements monad
(ns frp.tuple
  (:require [aid.core :as aid]
            [cats.context :as ctx]
            [cats.core :as m]
            [cats.protocols :as p]
            [cats.util :as util]
            [frp.helpers :as helpers :include-macros true]))

(declare ->Tuple)
;In ClojureScript declare works around the following warning:
;WARNING: Use of undeclared Var frp.tuple/->Tuple

(defrecord Tuple
  [fst snd]
  p/Contextual
  (-get-context [_]
    (helpers/reify-monad (fn [f _]
                           (Tuple. fst (f snd)))
                         (partial ->Tuple (-> fst
                                              ctx/infer
                                              m/mempty))
                         (comp (aid/build Tuple.
                                          (comp (partial m/<> fst)
                                                :fst)
                                          :snd)
                               :snd)))
  p/Printable
  (-repr [_]
    (str "#[tuple " (pr-str fst) " " (pr-str snd) "]")))

(util/make-printable Tuple)

(def tuple
  ->Tuple)

(def snd
  :snd)

(def fst
  :fst)
