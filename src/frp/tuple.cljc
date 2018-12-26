;TODO use cats.data when pair implements monad
(ns frp.tuple
  (:require [aid.core :as aid]
            [cats.context :as ctx]
            [cats.core :as m]
            [cats.protocols :as p]
            [cats.util :as util])
  #?(:cljs (:require-macros frp.tuple)))

(declare ->Tuple)
;In ClojureScript declare works around the following warning:
;WARNING: Use of undeclared Var frp.tuple/->Tuple

(defmacro reify-monad
  [fmap pure join & more]
  `(reify
     p/Context
     p/Functor
     ;Method signatures are quoted to avoid warnings.
     ;WARNING: Bad method signature in protocol implementation, cats.protocols/Functor does not declare method called frp.helpers/-fmap
     (~'-fmap [_# f# fa#]
       (~fmap f# fa#))
     p/Applicative
     (~'-pure [_# v#]
       (~pure v#))
     (~'-fapply [_# fab# fa#]
       (aid/ap fab# fa#))
     p/Monad
     (~'-mreturn [_# a#]
       (~pure a#))
     (~'-mbind [_# ma# f#]
       ;TODO thread this form
       (~join (m/<$> f# ma#)))
     ~@more))

(defrecord Tuple
  [fst snd]
  p/Contextual
  (-get-context [_]
    (reify-monad (fn [f _]
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
