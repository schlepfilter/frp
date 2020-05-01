(ns frp.ajax
  (:require [aid.core :as aid]
            [ajax.core :as ajax]
            [com.rpl.specter :as s]
            [frp.derived :as derived]
            [frp.primitives.event :as event]))

(aid/defcurried request
  [f url option]
  ;TODO make option optional
  (event/effect #(->> option
                      (merge {:handler identity})
                      (s/transform :handler (partial comp %))
                      (f url))
                (derived/event)))

(def GET
  (request ajax/GET))

(def POST
  (request ajax/POST))
