(ns examples.cycle.http-search-github
  (:require [clojure.walk :as walk]
            [cats.core :as m]
            [frp.ajax :refer [GET]]
            [frp.clojure.core :as core]
            [frp.core :as frp]))

(frp/defe term)

(def endpoint
  "https://api.github.com/search/repositories")

(def response
  (->> term
       (core/remove empty?)
       (m/=<< (comp (partial GET endpoint)
                    (partial assoc-in
                             {:handler walk/keywordize-keys}
                             [:params :q])))))

(def users
  (->> response
       (m/<$> :items)
       (frp/stepper [])))

(defn http-search-github-component
  [users*]
  [:div
   [:label "Search:"]
   [:input {:on-change #(-> %
                            .-target.value
                            term)
            :type      "text"}]
   (->> users*
        (map (fn [user*]
               [:li
                [:a {:href (:html_url user*)}
                 (:name user*)]]))
        (cons :ul)
        vec)])

(def http-search-github
  (m/<$> http-search-github-component users))
