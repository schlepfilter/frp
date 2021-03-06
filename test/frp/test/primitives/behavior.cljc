(ns frp.test.primitives.behavior
  (:require [aid.core :as aid :include-macros true]
            [aid.unit :as unit]
            [cats.context :as ctx]
            [cats.core :as m]
            [clojure.test.check.generators :as gen]
            [#?(:clj  clojure.test
                :cljs cljs.test) :as test :include-macros true]
            [clojure.test.check]
            [clojure.test.check.clojure-test
             :as clojure-test
             :include-macros true]
            [frp.core :as frp]
            [frp.helpers :as helpers]
            [frp.tuple :as tuple]
            [frp.test.helpers :as test-helpers :include-macros true]))

(clojure-test/defspec time-increasing
  test-helpers/cljc-num-tests
  (test-helpers/set-up-for-all [advance1 test-helpers/advance
                                advance2 test-helpers/advance]
                               (frp/activate)
                               (advance1)
                               (let [t @frp/time]
                                 (advance2)
                                 (helpers/<= t @frp/time))))

(clojure-test/defspec <$>-identity
  test-helpers/cljc-num-tests
  (test-helpers/set-up-for-all
    ;TODO generate a behavior by stepper and call the event
    [input-behavior test-helpers/any-behavior
     f (test-helpers/function test-helpers/any-equal)]
    (let [fmapped-behavior (m/<$> f input-behavior)]
      (frp/activate)
      (->> @input-behavior
           f
           (= @fmapped-behavior)))))

(clojure-test/defspec pure-identity
  test-helpers/cljc-num-tests
  (test-helpers/set-up-for-all [a test-helpers/any-equal]
                               (= @(-> unit/unit
                                       frp/behavior
                                       ctx/infer
                                       (m/pure a))
                                  a)))

(def get-behaviors
  #(gen/let [bs (gen/sized (partial gen/vector test-helpers/any-behavior))
             as (gen/vector test-helpers/any-equal (count %))]
     (->> %
          (map frp/stepper as)
          (concat bs)
          gen/shuffle)))

(def join-generator
  (gen/let [probabilities* (test-helpers/probabilities 1)
            inner-events (gen/return (test-helpers/get-events probabilities*))
            as (gen/vector test-helpers/any-equal (count inner-events))
            [inner-behavior & _ :as inner-behaviors]
            (get-behaviors inner-events)
            ;TODO generate a behavior with pure
            outer-event (gen/return (frp/event))
            ;TODO create a behavior with pure
            outer-behavior (gen/return (frp/stepper inner-behavior outer-event))
            ;TODO shuffle calls
            calls (gen/return (concat (map partial inner-events as)
                                      (map #(partial outer-event %)
                                           inner-behaviors)))]
    (gen/tuple (gen/return inner-behaviors)
               (gen/return outer-behavior)
               (gen/return calls))))

(clojure-test/defspec join-identity
  test-helpers/cljc-num-tests
  (test-helpers/set-up-for-all
    [[inner-behaviors outer-behavior calls] join-generator]
    (let [joined-behavior (m/join outer-behavior)]
      (frp/activate)
      (test-helpers/run-calls! calls)
      (= @joined-behavior @(last inner-behaviors)))))


(clojure-test/defspec stepper-identity
  test-helpers/cljc-num-tests
  (test-helpers/set-up-for-all [a test-helpers/any-equal
                                as (gen/vector test-helpers/any-equal)
                                e test-helpers/any-event]
                               (let [b (frp/stepper a e)
                                     occurrences (concat [a]
                                                         (map tuple/snd @e)
                                                         as)]
                                 (frp/activate)
                                 (run! e as)
                                 (= @b (last occurrences)))))

;TODO test applicative
