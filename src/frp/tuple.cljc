;TODO use cats.data when pair implements monad
(ns frp.tuple
  (:require [aid.core :as aid]
            [cats.context :as ctx]
            [cats.core :as m]
            [cats.protocols :as p]
            [cats.util :as util]))

(declare ->Tuple)
;In ClojureScript declare works around the following warning:
;WARNING: Use of undeclared Var frp.tuple/->Tuple

(defrecord Tuple
  [fst snd]
  p/Contextual
  (-get-context [_]
    (reify
      p/Context
      p/Functor
      (-fmap [_ f _]
        (Tuple. fst (f snd)))
      p/Applicative
      (-pure [_ a]
        (->Tuple (-> fst
                     ctx/infer
                     m/mempty)
                 a))
      (-fapply [_ fab fa]
        (aid/ap fab fa))
      p/Monad
      (-mreturn [context a]
        (p/-pure context a))
      (-mbind [_ ma f]
        ((aid/build Tuple.
                    (comp (partial m/<> fst)
                          :fst)
                    :snd)
          (:snd (m/<$> f ma))))))
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
