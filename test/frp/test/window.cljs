(ns frp.test.window
  (:require [clojure.test.check]
            [clojure.test.check.clojure-test
             :as clojure-test
             :include-macros true]
            [frp.core :as frp]
            [frp.window :as window]
            [frp.test.helpers :as helpers :include-macros true]))

(clojure-test/defspec window
  helpers/cljs-num-tests
  (helpers/set-up-for-all [advance* helpers/advance]
                          (frp/activate)
                          (advance*)
                          (and (= @window/inner-height js/window.innerHeight)
                               (= @window/inner-width js/window.innerWidth)
                               (= @window/outer-height js/window.outerHeight)
                               (= @window/outer-width js/window.outerWidth)
                               (= @window/scroll-x js/window.scrollX)
                               (= @window/scroll-y js/window.scrollY))))
