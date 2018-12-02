(ns frp.test.primitives.event
  (:refer-clojure :exclude [transduce])
  (:require [aid.core :as aid]
            [aid.unit :as unit]
            [#?(:clj  clojure.test
                :cljs cljs.test) :as test :include-macros true]
            [cats.monad.maybe :as maybe]
            [clojure.test.check]
            [clojure.test.check.clojure-test
             :as clojure-test
             :include-macros true]
            [clojure.test.check.generators :as gen]
            [frp.core :as frp]
            [frp.helpers :as helpers]
            [frp.primitives.event :as event]
            [frp.time :as time]
            [frp.tuple :as tuple]
            [frp.test.helpers :as test-helpers :include-macros true]))

(test/use-fixtures :each test-helpers/fixture)

(clojure-test/defspec
  call-inactive
  test-helpers/cljc-num-tests
  (test-helpers/restart-for-all [as (gen/vector test-helpers/any-equal)]
                                (let [e (frp/event)]
                                  (run! e as)
                                  (= @e []))))

(clojure-test/defspec
  call-active
  test-helpers/cljc-num-tests
  (test-helpers/restart-for-all [as (gen/vector test-helpers/any-equal)]
                                (let [e (frp/event)]
                                  (frp/activate)
                                  (run! e as)
                                  (->> [(map tuple/snd @e) as]
                                       (map last)
                                       (apply =)))))

(clojure-test/defspec
  event-pure
  test-helpers/cljc-num-tests
  (test-helpers/restart-for-all [a test-helpers/any-equal]
                                (= (last @(-> (frp/event)
                                              aid/infer
                                              (aid/pure a)))
                                   (-> 0
                                       time/time
                                       (tuple/tuple a)))))

;TODO test monad laws with join

(def <>
  ;TODO refactor
  (gen/let [probabilities (gen/vector test-helpers/probability 2)
            [input-events fmapped-events]
            (test-helpers/events-tuple probabilities)
            ns (gen/vector (gen/sized (partial gen/choose 0))
                           (count input-events))
            calls (gen/shuffle (mapcat (fn [n e]
                                         (repeat n
                                                 (partial e unit/unit)))
                                       ns
                                       input-events))]
           (gen/tuple (gen/return fmapped-events)
                      (gen/return (apply aid/<> fmapped-events))
                      (gen/return (partial run!
                                           aid/funcall
                                           calls)))))

(def get-last-occs
  (helpers/if-else
    empty?
    #(filter (comp (partial =
                            (-> %
                                last
                                tuple/fst))
                   tuple/fst)
             %)))

(clojure-test/defspec
  event-<>
  test-helpers/cljc-num-tests
  (test-helpers/restart-for-all [[fmapped-events mappended-event call] <>]
                                (frp/activate)
                                (call)
                                (->> fmapped-events
                                     (map deref)
                                     (apply event/merge-occs)
                                     get-last-occs
                                     (= (get-last-occs @mappended-event)))))

(defn get-generators
  [generator xforms**]
  (map (partial (aid/flip gen/fmap) generator) xforms**))

(def any-nilable-equal
  (gen/one-of [test-helpers/any-equal (gen/return nil)]))

(def xform*
  (gen/one-of
    (concat (map (comp gen/return
                       aid/funcall)
                 [dedupe distinct])
            (get-generators gen/s-pos-int [take-nth partition-all])
            (get-generators gen/int [drop take])
            (get-generators (test-helpers/function gen/boolean)
                            [drop-while filter remove take-while])
            (get-generators (test-helpers/function test-helpers/any-equal)
                            [map map-indexed partition-by])
            (get-generators (test-helpers/function any-nilable-equal)
                            [keep keep-indexed])
            [(gen/fmap replace (gen/map test-helpers/any-equal
                                        test-helpers/any-equal))
             (gen/fmap interpose test-helpers/any-equal)]
            ;Composing mapcat more than once seems to make the test to run longer than 10 seconds.
            ;[(gen/fmap mapcat (test-helpers/function (gen/vector test-helpers/any-equal)))]
            )))

(def xform
  (->> xform*
       gen/vector
       gen/not-empty
       (gen/fmap (partial apply comp))))

(defn get-elements
  [xf earliests as]
  (maybe/map-maybe (partial (comp unreduced
                                  (xf (comp maybe/just
                                            second
                                            vector)))
                            aid/nothing)
                   (concat earliests as)))

(clojure-test/defspec
  transduce-identity
  test-helpers/cljc-num-tests
  ;TODO refactor
  (test-helpers/restart-for-all
    [input-event test-helpers/event
     xf xform
     f (test-helpers/function test-helpers/any-equal)
     init test-helpers/any-equal
     as (gen/vector test-helpers/any-equal)]
    (let [transduced-event (frp/transduce xf f init input-event)
          earliests @input-event]
      (frp/activate)
      (run! input-event as)
      (or (empty? @transduced-event)
          (->> as
               (get-elements xf (map tuple/snd earliests))
               (reductions f init)
               (take-last (count @transduced-event))
               (map tuple/snd @transduced-event))))))

(clojure-test/defspec
  cat-identity
  test-helpers/cljc-num-tests
  ;TODO refactor
  (test-helpers/restart-for-all
    ;TODO generate an event with pure
    [input-event (gen/fmap (fn [_]
                             (frp/event))
                           (gen/return unit/unit))
     f (test-helpers/function test-helpers/any-equal)
     init test-helpers/any-equal
     ;TODO generate list
     as (gen/vector (gen/vector test-helpers/any-equal))]
    ;TODO compose xforms
    (let [cat-event (frp/transduce cat f init input-event)
          map-event (frp/transduce (comp (remove empty?)
                                         (map last))
                                   f
                                   init
                                   input-event)]
      (frp/activate)
      (run! input-event as)
      (= @cat-event @map-event))))

;TODO test snapshot
