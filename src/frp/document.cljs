(ns frp.document
  (:require [frp.browser :as browser :include-macros true]))

(browser/defevent visibilitychange
  #(merge (browser/convert %)
          (browser/convert js/document)))

(browser/defbehavior hidden
  visibilitychange)
