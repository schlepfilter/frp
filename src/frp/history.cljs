(ns frp.history
  (:require [frp.browser :as browser]
            [frp.primitives.behavior :as behavior :include-macros true]
            [frp.primitives.event :as event]))

(def pushstate
  (event/->Event ::pushstate))

(behavior/register
  (browser/redef pushstate))

(defn push-state
  [state title url-string]
  (js/history.pushState state title url-string)
  (pushstate {:location {:pathname js/location.pathname}}))
