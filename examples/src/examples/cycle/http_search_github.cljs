(ns examples.cycle.http-search-github
  (:require [clojure.walk :as walk]
            [ajax.core :refer [GET]]
            [cats.core :as m]
            [frp.clojure.core :as core]
            [frp.core :as frp]))

(def term
  (frp/event))

(def response
  ;TODO use >>= and ajax event
  (frp/event))

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

(def endpoint
  "https://api.github.com/search/repositories")

(def option
  (->> term
       (core/remove empty?)
       (m/<$> (partial assoc-in
                       {:handler (comp response
                                       walk/keywordize-keys)}
                       [:params :q]))))

(frp/on (partial GET endpoint) option)
