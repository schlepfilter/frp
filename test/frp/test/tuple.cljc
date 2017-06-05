(ns frp.test.tuple
  (:require [cats.builtin]
            [cats.monad.maybe :as maybe]
    ;clojure.test.check is required to avoid the following warning.
    ;Figwheel: Watching build - test
    ;Figwheel: Cleaning build - test
    ;Compiling "resources/public/test/js/main.js" from ["src" "test"]...
    ;WARNING: Use of undeclared Var clojure.test.check/quick-check
            [clojure.test.check]
            [clojure.test.check.clojure-test
             :as clojure-test
             :include-macros true]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop :include-macros true]
            [help.core :as help]
            [help.unit :as unit]
            [frp.tuple :as tuple]
            [frp.test.helpers :as helpers]))

(def maybe
  (partial (help/flip gen/bind)
           (comp gen/elements
                 (partial vector help/nothing)
                 maybe/just)))

(def scalar-monoids
  [gen/string
   (gen/return unit/unit)
   (gen/vector helpers/any-equal)
   (gen/list helpers/any-equal)
   (gen/set helpers/any-equal)
   (gen/map helpers/any-equal helpers/any-equal)])

(def scalar-monoid
  (gen/one-of scalar-monoids))

(def monoid
  (gen/one-of [scalar-monoid
               (gen/recursive-gen maybe scalar-monoid)]))

(def mempty
  (gen/fmap (comp help/mempty help/infer)
            monoid))

(defn scalar-monoid-vector
  [n]
  (gen/one-of (map (partial (help/flip gen/vector) n)
                   scalar-monoids)))

(clojure-test/defspec
  monad-right-identity-law
  helpers/cljc-num-tests
  (prop/for-all [a helpers/any-equal
                 mempty* mempty]
                (= (help/>>= (tuple/tuple mempty* a) help/return)
                   (tuple/tuple mempty* a))))

(clojure-test/defspec
  monad-left-identity-law
  helpers/cljc-num-tests
  (prop/for-all [a helpers/any-equal
                 f* (helpers/function helpers/any-equal)
                 monoid* monoid]
                (let [f (comp (partial tuple/tuple monoid*)
                              f*)]
                  (= (help/>>= (tuple/tuple (-> monoid*
                                                help/infer
                                                help/mempty)
                                            a)
                               f)
                     (f a)))))

(clojure-test/defspec
  monad-associativity-law
  helpers/cljc-num-tests
  (prop/for-all [a helpers/any-equal
                 monoids (scalar-monoid-vector 3)
                 f* (helpers/function helpers/any-equal)
                 g* (helpers/function helpers/any-equal)]
                (let [f (comp (partial tuple/tuple (second monoids))
                              f*)
                      g (comp (partial tuple/tuple (last monoids))
                              g*)
                      ma (tuple/tuple (first monoids) a)]
                  (= (help/->= ma f g)
                     (help/>>= ma (comp (partial help/=<< g)
                                        f))))))
