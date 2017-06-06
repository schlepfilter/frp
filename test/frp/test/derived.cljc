(ns frp.test.derived
  (:require [#?(:clj  clojure.test
                :cljs cljs.test) :as test :include-macros true]
            [cats.monad.maybe :as maybe]
            [clojure.test.check]
            [clojure.test.check.clojure-test
             :as clojure-test
             :include-macros true]
            [clojure.test.check.generators :as gen]
            [aid.core :as help :include-macros true]
            [frp.core :as frp]
            [frp.tuple :as tuple]
            [frp.test.helpers :as helpers :include-macros true]))

(test/use-fixtures :each helpers/fixture)

(def switcher
  ;TODO refactor
  (gen/let [probabilities (helpers/probabilities 4)
            [[input-event & input-events]
             [fmapped-switching-event
              fmapped-outer-event
              & fmapped-inner-events]]
            (helpers/events-tuple probabilities)
            [stepper-outer-any input-outer-any]
            (gen/vector helpers/any-equal 2)
            ;TODO call <$> on outer-behavior
            outer-behavior (gen/elements [(frp/stepper stepper-outer-any
                                                       fmapped-outer-event)
                                          frp/time])
            stepper-inner-anys (gen/vector helpers/any-equal
                                           (count fmapped-inner-events))
            steps (gen/vector gen/boolean (count fmapped-inner-events))
            ;TODO call <$> on inner-behaviors
            inner-behaviors
            (gen/return
              (doall
                (map (fn [step stepping-inner-any fmapped-inner-event]
                       (if step
                         (frp/stepper stepping-inner-any
                                      fmapped-inner-event)
                         frp/time))
                     steps
                     stepper-inner-anys
                     fmapped-inner-events)))
            switching-event
            (gen/return (help/<$>
                          (helpers/make-iterate inner-behaviors)
                          fmapped-switching-event))
            input-event-anys (gen/vector helpers/any-equal
                                         ((help/casep @switching-event
                                                      empty? identity
                                                      dec)
                                           (count fmapped-inner-events)))
            input-events-anys (gen/vector helpers/any-equal
                                          (count input-event-anys))
            calls (->> (map (fn [input-event* a]
                              (help/maybe-if-not (= input-event*
                                                    input-event)
                                                 (partial input-event* a)))
                            input-events
                            input-events-anys)
                       maybe/cat-maybes
                       (concat (map (fn [a]
                                      (partial input-event a))
                                    input-event-anys))
                       gen/shuffle)]
           (gen/tuple (gen/return outer-behavior)
                      (gen/return switching-event)
                      (gen/return (frp/switcher outer-behavior switching-event))
                      (->> calls
                           (map help/funcall)
                           (partial doall)
                           gen/return))))

(clojure-test/defspec
  switcher-identity
  helpers/cljc-num-tests
  (helpers/restart-for-all
    ;TODO refactor
    ;Push-Pull Functional Reactive Programming calls the behavior returned by switcher the switcher behavior.
    [[outer-behavior e switcher-behavior calls] switcher]
    (frp/activate)
    (calls)
    (= @switcher-behavior @(->> @e
                                (map tuple/snd)
                                (cons outer-behavior)
                                last))))
