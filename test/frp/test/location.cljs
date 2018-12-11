(ns frp.test.location
  (:require [cljs.test :as test]
            [clojure.test.check]
            [clojure.test.check.clojure-test
             :as clojure-test
             :include-macros true]
            [frp.core :as frp]
            [frp.location :as location]
            [frp.test.helpers :as helpers :include-macros true]))

(test/use-fixtures :each helpers/fixture)

(clojure-test/defspec
  location
  helpers/cljs-num-tests
  (helpers/restart-for-all [advance* helpers/advance]
                           (frp/activate)
                           (advance*)
                           ;TODO call on if asynchronous testing gets supported in test.check
                           (= @location/pathname js/location.pathname)))
