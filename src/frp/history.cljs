(ns frp.history
  (:require [frp.browser :as browser :include-macros true]))

(browser/defevent pushstate)

(defn push-state
  [state title url-string]
  (js/history.pushState state title url-string)
  (pushstate {:location {:pathname js/location.pathname}}))
