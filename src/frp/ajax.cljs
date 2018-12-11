(ns frp.ajax
  (:require [ajax.core :as ajax]
            [com.rpl.specter :as s :include-macros true]
            [frp.browser :as browser]
            [frp.derived :as derived]))

(defn make-request
  [f]
  (fn [url option]
    (browser/effect #(->> option
                          (merge {:handler identity})
                          (s/transform :handler (partial comp %))
                          (f url))
                    (derived/event))))

(def GET
  (make-request ajax/GET))
