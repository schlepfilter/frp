(ns frp.window
  (:refer-clojure :exclude [drop])
  (:require [frp.browser :as browser :include-macros true]))

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
  (browser/make-convert-merge js/location))

(browser/defevent resize
  (browser/make-convert-merge js/window))

(browser/defevent scroll
  browser/convert)

(browser/defevent submit
  browser/convert)

(browser/defevent wheel
  browser/convert)

(browser/defbehavior inner-height
  resize)

(browser/defbehavior inner-width
  resize)

(browser/defbehavior outer-width
  resize)

(browser/defbehavior outer-height
  resize)

(browser/defbehavior scroll-x
  resize)

(browser/defbehavior scroll-y
  resize)
