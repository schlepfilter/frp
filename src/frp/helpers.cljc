(ns frp.helpers
  (:refer-clojure :exclude [defcurried <=])
  (:require [aid.core :as aid :include-macros true]
            [cats.core :as m]
            [cats.protocols :as p]
            [com.rpl.specter :as s]))

#?(:clj (defmacro reify-monad
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
               (~join (m/<$> f# ma#)))
             ~@more)))

(def <=
  (comp (partial every? (comp not
                              pos?
                              (partial apply compare)))
        (partial partition 2 1)
        vector))
