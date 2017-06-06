(ns frp.helpers
  (:refer-clojure :exclude [defcurried])
  (:require [aid.core :as aid :include-macros true]
            [cats.protocols :as p]
            [com.rpl.specter :as s]))

(defn if-then-else
  [if-function then-function else]
  ((aid/build if
              if-function
              then-function
              identity)
    else))

#?(:clj (defmacro reify-monad
          [pure mbind & more]
          `(reify
             p/Context
             p/Functor
             (~'-fmap [_# f# fa#]
               ;TODO remove 1
               ((aid/lift-m 1 f#) fa#))
             p/Applicative
             (~'-pure [_# v#]
               (~pure v#))
             (~'-fapply [_# fab# fa#]
               (aid/ap fab# fa#))
             p/Monad
             (~'-mreturn [_# a#]
               (~pure a#))
             (~'-mbind [_# ma# f#]
               (~mbind ma# f#))
             ~@more)))

(def call-functions
  (aid/flip (partial reduce (aid/flip aid/funcall))))
