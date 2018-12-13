(ns frp.location
  (:require [cats.core :as m]
            [frp.browser :as browser :include-macros true]
            [frp.history :as history]
            [frp.window :as window]))

(browser/defbehavior pathname
  (m/<> window/popstate history/pushstate))
