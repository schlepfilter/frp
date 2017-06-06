(ns examples.cycle.http-search-github
  (:require [clojure.walk :as walk]
            [aid.core :as aid]
            [ajax.core :refer [GET]]
            [frp.clojure.core :as core]
            [frp.core :as frp]))

(def term
  (frp/event))

(def response
  (frp/event))

(def users
  (->> response
       (aid/<$> :items)
       (frp/stepper [])))

(defn http-search-github-component
  [users*]
  [:div
   [:label "Search:"]
   [:input {:on-change (fn [event*]
                         (-> event*
                             .-target.value
                             term))
            :type      "text"}]
   (->> users*
        (map (fn [user*]
               [:li
                [:a {:href (:html_url user*)}
                 (:name user*)]]))
        (cons :ul)
        vec)])

(def http-search-github
  (aid/<$> http-search-github-component users))

(def endpoint
  "https://api.github.com/search/repositories")

(def option
  (->> term
       (core/remove empty?)
       (aid/<$> (partial assoc-in
                         {:handler (comp response
                                          walk/keywordize-keys)}
                         [:params :q]))))

(frp/on (partial GET endpoint) option)
