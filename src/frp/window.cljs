(ns frp.window
  (:refer-clojure :exclude [drop])
  (:require [cats.core :as m]
            [cuerdas.core :as cuerdas]
            [goog.object :as object]
            [oops.core :refer [oget+]]
            [frp.browser :as browser :include-macros true]
            [frp.primitives.behavior :as behavior :include-macros true]))

(defn convert
  [x]
  (->> x
       object/getKeys
       (mapcat (juxt (comp keyword
                           cuerdas/kebab)
                     #(case (-> x
                                (oget+ %)
                                goog/typeOf)
                        "function" (partial js-invoke x %)
                        (oget+ x %))))
       (apply hash-map)))

;Defining dragover is visiliby slower possibly because it fires every few milliseconds.
;(browser/defevent dragover
;  convert)

(browser/defevent dragstart
  convert)

(browser/defevent drop
  convert)

(browser/defevent pointermove
  convert)

(browser/defevent pointerup
  convert)

(browser/defevent popstate
  (fn [_]
    (convert js/location)))

(browser/defevent resize
  (fn [_]
    (convert js/window)))

(browser/defbehavior inner-height
  #(->> resize
        (m/<$> :inner-height)
        (behavior/stepper js/innerHeight)))
