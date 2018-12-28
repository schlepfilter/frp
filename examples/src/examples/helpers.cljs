(ns examples.helpers
  (:require [aid.core :as aid]
            [frp.core :as frp]
            [thi.ng.color.core :as col]))

(def get-color
  (comp deref
        col/as-css
        col/hsla))

(frp/defe first-name last-name)

(def full-name
  ((aid/lift-a str)
    (frp/stepper "" first-name)
    (frp/behavior " ")
    (frp/stepper "" last-name)))
