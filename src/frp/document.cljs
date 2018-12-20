(ns frp.document
  (:require [frp.browser :as browser]))

(browser/defevent visibilitychange
  (browser/make-convert-merge js/document))

(browser/defbehavior hidden
  visibilitychange)

(browser/defbehavior visibility-state
  visibilitychange)
