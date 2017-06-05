(ns frp.test.io
  (:require [clojure.test.check]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop :include-macros true]
            [clojure.test.check.clojure-test
             :as clojure-test
             :include-macros true]
            [#?(:clj  clojure.test
                :cljs cljs.test) :as test :include-macros true]
            [com.rpl.specter :as s]
            [help.core :as help]
            [frp.core :as frp]
            [frp.tuple :as tuple]
            [frp.test.helpers :as helpers :include-macros true]
    #?(:clj
            [riddley.walk :as walk]))
  #?(:cljs (:require-macros [frp.test.io :refer [with-exitv]])))

(test/use-fixtures :each helpers/fixture)

#?(:clj (defmacro with-exitv
          [exit-name & body]
          (potemkin/unify-gensyms
            `(let [exits-state## (atom [])]
               ~(walk/walk-exprs
                  (partial = exit-name)
                  (fn [_#]
                    `(comp (partial swap! exits-state##)
                           (help/curry 2 (help/flip conj))))
                  (cons `do body))
               @exits-state##))))

(clojure-test/defspec
  with-exitv-identity
  helpers/cljc-num-tests
  (prop/for-all [as (gen/vector helpers/any-equal)
                 b helpers/any-equal]
                (= (with-exitv exit
                               (->> as
                                    (map exit)
                                    doall)
                               b)
                   as)))

(clojure-test/defspec
  event-on
  helpers/cljc-num-tests
  (helpers/restart-for-all
    [e helpers/event
     as (gen/vector helpers/any-equal)]
    (= (vec (concat (map tuple/snd @e)
                    as))
       (with-exitv exit
                   (frp/on exit e)
                   (frp/activate)
                   (run! e as)))))

(clojure-test/defspec
  behavior-on
  helpers/cljc-num-tests
  (helpers/restart-for-all
    [e helpers/event
     a helpers/any-equal
     as (gen/vector helpers/any-equal)]
    (let [b (frp/stepper a e)]
      (= (vec (dedupe (concat [a]
                              (remove (partial = a)
                                      (map tuple/snd @e))
                              as)))
         (with-exitv exit
                     (frp/on exit b)
                     (frp/activate)
                     (run! e as))))))
