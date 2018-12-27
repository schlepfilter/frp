(ns examples.cycle.nested-folders
  (:require [aid.core :as aid]
            [cats.core :as m]
            [com.rpl.specter :as s]
            [frp.core :as frp]
            [thi.ng.color.core :as col]))

(frp/defe addition removal)

(defn get-folder
  []
  {:path  []
   :color @(col/as-css (col/random-rgb))
   :child {}})

(def initial-folder
  (get-folder))

(def tree
  (->> addition
       (m/<$> (aid/curriedfn [path* tree*]
                             (let [path** [path* :child (-> (random-uuid)
                                                            str
                                                            keyword)]]
                               (s/setval path**
                                         (s/setval :path
                                                   path**
                                                   (get-folder))
                                         tree*))))
       (m/<> (m/<$> #(partial s/setval* % s/NONE) removal))
       (frp/accum initial-folder)
       (frp/stepper initial-folder)))

(defn nested-folders-component
  [{:keys [path color child]}]
  [:div {:style {:background-color color
                 :border           "2px solid black"
                 :padding          "2em"
                 :width            "auto"}}
   [:button {:on-click #(addition path)}
    "Add folder"]
   (aid/casep path
     empty? [:div]
     [:button {:on-click #(removal path)}
      "Remove me"])
   (->> child
        vals
        (mapv nested-folders-component)
        (s/setval s/BEFORE-ELEM :div))])

(def nested-folders
  (m/<$> nested-folders-component tree))
