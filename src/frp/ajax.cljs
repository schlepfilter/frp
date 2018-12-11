(ns frp.ajax
  (:require [aid.core :as aid]
            [ajax.core :as ajax]
            [com.rpl.specter :as s :include-macros true]
            [frp.browser :as browser]
            [frp.derived :as derived]))

(aid/defcurried request
  [f url option]
  (browser/effect #(->> option
                        (merge {:handler identity})
                        (s/transform :handler (partial comp %))
                        (f url))
                  (derived/event)))

(def GET
  (request ajax/GET))
