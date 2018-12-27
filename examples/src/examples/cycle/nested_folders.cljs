(ns examples.cycle.nested-folders
  (:require [aid.core :as aid]
            [cats.core :as m]
            [com.rpl.specter :as s]
            [frp.core :as frp]
            [thi.ng.color.core :as col]))

(frp/defe addition removal)

(defn get-folder
  []
  {:path     []
   :color    @(col/as-css (col/random-rgb))
   :children []})

(def initial-folder
  (get-folder))

(def tree
  (->> addition
       (m/<$> (aid/curriedfn [path* tree*]
                             (s/setval (s/setval s/END
                                                 [:children s/AFTER-ELEM]
                                                 path*)
                                       (s/setval :path
                                                 [path*
                                                  :children
                                                  (->> tree*
                                                       (s/select-one* path*)
                                                       :children
                                                       count
                                                       s/nthpath)]
                                                 (get-folder))
                                       tree*)))
       (frp/accum initial-folder)
       (frp/stepper initial-folder)))

(defn nested-folders-component
  [{:keys [path color children]}]
  [:div {:style {:background-color color
                 :border           "2px solid black"
                 :padding          "2em"
                 :width            "auto"}}
   [:button {:on-click #(addition path)}
    "Add folder"]
   [:button {:on-click #(removal path)}
    "Remove me"]
   (->> children
        (mapv nested-folders-component)
        (s/setval s/BEFORE-ELEM :div))])

(def nested-folders
  (m/<$> nested-folders-component tree))
