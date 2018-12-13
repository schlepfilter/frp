(ns frp.document
  (:require [frp.browser :as browser :include-macros true]))

(browser/defevent visibilitychange
  (browser/make-convert-merge js/document))

(browser/defbehavior hidden
  visibilitychange)
