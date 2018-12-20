(ns frp.location
  (:refer-clojure :exclude [hash])
  (:require [cats.core :as m]
            [frp.browser :as browser]
            [frp.history :as history]
            [frp.window :as window]))

(def get-push-pop-state
  #(m/<> history/pushstate window/popstate))

(browser/defbehavior hash
  (get-push-pop-state))

(browser/defbehavior href
  (get-push-pop-state))

(browser/defbehavior pathname
  (get-push-pop-state))

(browser/defbehavior search
  (get-push-pop-state))
