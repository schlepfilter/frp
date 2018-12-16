(ns frp.test.location
  (:require [clojure.test.check]
            [clojure.test.check.clojure-test
             :as clojure-test
             :include-macros true]
            [frp.core :as frp]
            [frp.location :as location]
            [frp.test.helpers :as helpers :include-macros true]))

(clojure-test/defspec location
  helpers/cljs-num-tests
  (helpers/restart-for-all [advance* helpers/advance]
                           (frp/activate)
                           (advance*)
                           ;TODO call on if asynchronous testing gets supported in test.check
                           (and (= @location/hash js/location.hash)
                                (= @location/href js/location.href)
                                (= @location/pathname js/location.pathname)
                                (= @location/search js/location.search))))
