(ns frp.test.primitives.behavior
  (:require [aid.core :as aid :include-macros true]
            [clojure.test.check.generators :as gen]
            [#?(:clj  clojure.test
                :cljs cljs.test) :as test :include-macros true]
            [clojure.test.check]
            [clojure.test.check.clojure-test
             :as clojure-test
             :include-macros true]
            [aid.unit :as unit]
            [frp.core :as frp]
            [frp.helpers :as helpers]
            [frp.tuple :as tuple]
            [frp.test.helpers :as test-helpers :include-macros true]))

(test/use-fixtures :each test-helpers/fixture)

(clojure-test/defspec
  behavior-return
  test-helpers/cljc-num-tests
  (test-helpers/restart-for-all [a test-helpers/any-equal]
                                (= @(-> unit/unit
                                        frp/behavior
                                        aid/infer
                                        (aid/return a))
                                   a)))

(clojure-test/defspec
  time-increasing
  test-helpers/cljc-num-tests
  (test-helpers/restart-for-all
    [advance1 test-helpers/advance
     advance2 test-helpers/advance]
    (frp/activate)
    (advance1)
    (let [t @frp/time]
      (advance2)
      (helpers/<= t @frp/time))))

(clojure-test/defspec
  stepper-identity
  test-helpers/cljc-num-tests
  (test-helpers/restart-for-all
    [a test-helpers/any-equal
     as (gen/vector test-helpers/any-equal)
     e test-helpers/event]
    (let [b (frp/stepper a e)
          occurrences (concat [a] (map tuple/snd @e) as)]
      (frp/activate)
      (run! e as)
      (= @b (last occurrences)))))

(def behavior->>=
  ;TODO refactor
  (gen/let [probabilities (gen/vector test-helpers/probability 2)
            [[input-outer-event input-inner-event]
             [fmapped-outer-event fmapped-inner-event]]
            (test-helpers/events-tuple probabilities)
            outer-any test-helpers/any-equal
            outer-behavior (gen/elements [(frp/stepper outer-any
                                                       fmapped-outer-event)
                                          frp/time])
            inner-any test-helpers/any-equal
            f (gen/elements [frp/behavior
                             (constantly (frp/stepper inner-any
                                                      fmapped-inner-event))
                             (constantly frp/time)])
            [input-outer-anys input-inner-anys]
            (gen/vector (gen/vector test-helpers/any-equal) 2)
            calls (gen/shuffle (concat (map (fn [a]
                                              #(input-outer-event a))
                                            input-outer-anys)
                                       (map (fn [a]
                                              #(input-inner-event a))
                                            input-inner-anys)))
            invocations (gen/vector gen/boolean (count calls))]
           (gen/tuple (gen/return outer-behavior)
                      (gen/return f)
                      (gen/return (partial doall (map (fn [invocation call]
                                                        (if invocation
                                                          (call)))
                                                      invocations
                                                      calls))))))

(clojure-test/defspec
  behavior->>=-identity
  test-helpers/cljc-num-tests
  (test-helpers/restart-for-all
    [[outer-behavior get-behavior call] behavior->>=]
    (let [bound-behavior (aid/>>= outer-behavior get-behavior)]
      (frp/activate)
      (call)
      (= @bound-behavior @(get-behavior @outer-behavior)))))

;TODO test time-transform
