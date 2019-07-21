(ns frp.window
  (:refer-clojure :exclude [drop])
  (:require [frp.browser :as browser]))

(browser/defevent blur
  browser/convert-object)

(browser/defevent click
  browser/convert-object)

(browser/defevent contextmenu
  browser/convert-object)

(browser/defevent copy
  browser/convert-object)

(browser/defevent cut
  browser/convert-object)

(browser/defevent dragend
  browser/convert-object)

;Defining dragover is visiliby slower possibly because it fires every few milliseconds.
;(browser/defevent dragover
;  convert)

(browser/defevent dragleave
  browser/convert-object)

(browser/defevent dragstart
  browser/convert-object)

(browser/defevent drop
  browser/convert-object)

(browser/defevent focus
  browser/convert-object)

(browser/defevent input
  browser/convert-object)

(browser/defevent keydown
  browser/convert-object)

(browser/defevent keypress
  browser/convert-object)

(browser/defevent keyup
  browser/convert-object)

(browser/defevent paste
  browser/convert-object)

(browser/defevent pointerdown
  browser/convert-object)

(browser/defevent pointermove
  browser/convert-object)

(browser/defevent pointerout
  browser/convert-object)

(browser/defevent pointerover
  browser/convert-object)

(browser/defevent pointerup
  browser/convert-object)

(browser/defevent popstate
  (browser/make-convert-merge js/location))

;The following definition gives an error.
;(browser/defevent resize
;  (browser/make-convert-merge js/window))
;@resize
;#object[RangeError RangeError: Maximum call stack size exceeded]
(browser/defevent resize
  #(merge (browser/convert-object %)
          (browser/convert-keys #{"innerHeight"
                                  "innerWidth"
                                  "outerHeight"
                                  "outerWidth"
                                  "scrollX"
                                  "scrollY"}
                                js/window)))

(browser/defevent scroll
  browser/convert-object)

(browser/defevent submit
  browser/convert-object)

(browser/defevent wheel
  browser/convert-object)

(browser/defbehavior inner-height
  resize)

(browser/defbehavior inner-width
  resize)

(browser/defbehavior outer-height
  resize)

(browser/defbehavior outer-width
  resize)

(browser/defbehavior scroll-x
  resize)

(browser/defbehavior scroll-y
  resize)
