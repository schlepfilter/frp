(ns frp.test.document
  (:require [clojure.test.check]
            [clojure.test.check.clojure-test
             :as clojure-test
             :include-macros true]
            [frp.core :as frp]
            [frp.document :as document]
            [frp.test.helpers :as helpers :include-macros true]))

(clojure-test/defspec document
  helpers/cljs-num-tests
  (helpers/set-up-for-all [advance* helpers/advance]
                          (frp/activate)
                          (advance*)
                          (and (= @document/hidden js/document.hidden)
                               (= @document/visibility-state
                                  js/document.visibilityState))))
