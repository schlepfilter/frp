(ns examples.cycle.checkbox
  (:require [cats.core :as m]
            [frp.clojure.core :as core]
            [frp.core :as frp :include-macros true]))

(frp/defe check)

(def checked
  (->> check
       core/count
       (m/<$> odd?)
       (frp/stepper false)))

(defn checkbox-component
  [checked*]
  [:div
   [:input {:on-change #(check)
            :type      "checkbox"}]
   "Toggle me"
   [:p (if checked*
         "ON"
         "off")]])

(def checkbox
  (m/<$> checkbox-component checked))
