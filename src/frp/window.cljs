(ns frp.window
  (:refer-clojure :exclude [drop])
  (:require [cats.core :as m]
            [frp.browser :as browser :include-macros true]
            [frp.primitives.behavior :as behavior :include-macros true]))

;Defining dragover is visiliby slower possibly because it fires every few milliseconds.
;(browser/defevent dragover
;  convert)

(browser/defevent copy
  browser/convert)

(browser/defevent cut
  browser/convert)

(browser/defevent dragstart
  browser/convert)

(browser/defevent drop
  browser/convert)

(browser/defevent paste
  browser/convert)

(browser/defevent pointermove
  browser/convert)

(browser/defevent pointerup
  browser/convert)

(browser/defevent popstate
  (fn [_]
    (browser/convert js/location)))

(browser/defevent resize
  (fn [_]
    (browser/convert js/window)))

(browser/defbehavior inner-height
  #(->> resize
        (m/<$> :inner-height)
        (behavior/stepper js/innerHeight)))
