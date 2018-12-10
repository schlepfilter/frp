(ns examples.helpers
  (:require [thi.ng.color.core :as col]))

(def get-color
  (comp deref
        col/as-css
        col/hsla))
