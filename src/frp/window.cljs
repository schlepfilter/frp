(ns frp.window
  (:refer-clojure :exclude [drop])
  (:require [cats.core :as m]
            [frp.browser :as browser :include-macros true]
            [frp.primitives.behavior :as behavior :include-macros true]))

(browser/defevent blur
  browser/convert)

(browser/defevent change
  browser/convert)

(browser/defevent copy
  browser/convert)

(browser/defevent cut
  browser/convert)

;Defining dragover is visiliby slower possibly because it fires every few milliseconds.
;(browser/defevent dragover
;  convert)

(browser/defevent dragstart
  browser/convert)

(browser/defevent drop
  browser/convert)

(browser/defevent focus
  browser/convert)

(browser/defevent input
  browser/convert)

(browser/defevent keydown
  browser/convert)

(browser/defevent keypress
  browser/convert)

(browser/defevent keyup
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

(browser/defevent submit
  browser/convert)

(browser/defbehavior inner-height
  #(->> resize
        (m/<$> :inner-height)
        (behavior/stepper js/innerHeight)))
