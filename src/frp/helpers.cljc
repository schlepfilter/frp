(ns frp.helpers
  (:refer-clojure :exclude [defcurried])
  (:require [cats.protocols :as p]
            [com.rpl.specter :as s]
            [help.core :as help]))

(defn if-then-else
  [if-function then-function else]
  ((help/build if
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
               ((help/lift-m 1 f#) fa#))
             p/Applicative
             (~'-pure [_# v#]
               (~pure v#))
             (~'-fapply [_# fab# fa#]
               (help/ap fab# fa#))
             p/Monad
             (~'-mreturn [_# a#]
               (~pure a#))
             (~'-mbind [_# ma# f#]
               (~mbind ma# f#))
             ~@more)))

(def call-functions
  (help/flip (partial reduce (help/flip help/funcall))))
