(ns frp.ajax
  (:require [ajax.core :as ajax]
            [com.rpl.specter :as s :include-macros true]
            [frp.derived :as derived]))

(def make-request
  #(fn [url option]
     (let [e (derived/event)]
       (->> option
            (merge {:handler identity})
            (s/transform :handler (partial comp e))
            (% url))
       ;TODO garbage collect
       e)))

(def GET
  (make-request ajax/GET))
