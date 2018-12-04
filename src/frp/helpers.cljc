(ns frp.helpers
  (:refer-clojure :exclude [defcurried <=])
  (:require [aid.core :as aid :include-macros true]
            [cats.protocols :as p]
            [com.rpl.specter :as s]))

(aid/defcurried if-then-else
                ;TODO move this function to aid
                [if-function then-function else-function x]
                ((aid/build if
                            if-function
                            then-function
                            else-function)
                  x))

(aid/defcurried if-then
                ;TODO move this function to aid
                [if-function then-function else]
                (if-then-else if-function then-function identity else))

(aid/defcurried if-else
                ;TODO move this function to aid
                [if-function else-function then]
                (if-then-else if-function identity else-function then))

#?(:clj (defmacro reify-monad
          [pure mbind & more]
          `(reify
             p/Context
             p/Functor
             (~'-fmap [_# f# fa#]
               ;TODO define mbind in terms of <$> and join
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
  (->> aid/funcall
       aid/flip
       (partial reduce)
       aid/flip))

(def <=
  (comp (partial every? (comp not
                              pos?
                              (partial apply compare)))
        (partial partition 2 1)
        vector))
