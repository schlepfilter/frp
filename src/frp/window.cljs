(ns frp.window
  (:refer-clojure :exclude [drop])
  (:require [cats.core :as m]
            [frp.browser :as browser :include-macros true]
            [frp.primitives.behavior :as behavior :include-macros true]))

(browser/defevent blur
  browser/convert)

(browser/defevent click
  browser/convert)

(browser/defevent contextmenu
  browser/convert)

(browser/defevent copy
  browser/convert)

(browser/defevent cut
  browser/convert)

(browser/defevent dragend
  browser/convert)

;Defining dragover is visiliby slower possibly because it fires every few milliseconds.
;(browser/defevent dragover
;  convert)

(browser/defevent dragleave
  browser/convert)

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

(browser/defevent pointerdown
  browser/convert)

(browser/defevent pointermove
  browser/convert)

(browser/defevent pointerout
  browser/convert)

(browser/defevent pointerover
  browser/convert)

(browser/defevent pointerup
  browser/convert)

(browser/defevent popstate
  #(merge (browser/convert %)
          (browser/convert js/location)))

(browser/defevent resize
  #(merge (browser/convert %)
          (browser/convert js/window)))

(browser/defevent submit
  browser/convert)

(browser/defbehavior inner-height
  #(->> resize
        (m/<$> :inner-height)
        (behavior/stepper js/innerHeight)))
