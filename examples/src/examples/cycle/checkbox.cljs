(ns examples.cycle.checkbox
  (:require [aid.unit :as unit]
            [cats.core :as m]
            [frp.clojure.core :as core]
            [frp.core :as frp]))

(def check
  (frp/event))

(def checked
  (->> check
       core/count
       (m/<$> odd?)
       (frp/stepper false)))

(defn checkbox-component
  [checked*]
  [:div
   [:input {:on-change #(check unit/unit)
            :type      "checkbox"}]
   "Toggle me"
   [:p (if checked*
         "ON"
         "off")]])

(def checkbox
  (m/<$> checkbox-component checked))
