(ns examples.intro
  (:require [clojure.walk :as walk]
            [aid.core :as aid :include-macros true]
            [aid.unit :as unit]
            [ajax.core :refer [GET POST]]
            [com.rpl.specter :as s]
            [frp.clojure.core :as core]
            [frp.core :as frp]))

(def link-style
  {:display     "inline-block"
   :margin-left "0.313em"})

(defn get-user-component
  [user* click]
  [:li {:style {:align-items "center"
                :display     "flex"
                :padding     "0.313em"
                :visibility  (aid/casep user*
                                        empty? "hidden"
                                        "visible")}}
   [:img {:src   (:avatar_url user*)
          :style {:border-radius "1.25em"
                  :height        "2.5em"
                  :width         "2.5em"}}]
   [:a {:href  (:html_url user*)
        :style link-style}
    (:login user*)]
   [:a {:href     "#"
        :on-click (fn [event*]
                    (.preventDefault event*)
                    (click unit/unit))
        :style    link-style}
    "x"]])

(def grey
  "hsl(0, 0%, 93%)")

(def response
  (frp/event))

(def beginning
  (frp/event 0))

(def suggestion-number
  3)

(def closings
  (repeatedly suggestion-number frp/event))

(def closing-counts
  (map (comp (partial frp/stepper 0)
             core/count)
       closings))

(def user-number
  30)

(def offset-counts
  (->> suggestion-number
       (quot user-number)
       (range 0 user-number)
       (map (fn [click-count offset]
              (aid/<$> (partial + offset) click-count))
            closing-counts)))

(def users
  (apply (aid/lift-a (fn [response* & offset-counts*]
                       (map (partial nth (cycle response*))
                            offset-counts*)))
         (frp/stepper (repeat user-number {}) response)
         offset-counts))

(defn handle-click
  [event*]
  (.preventDefault event*)
  (response (repeat user-number {}))
  (->> (js/Math.random)
       (* 500)
       int
       beginning))

(defn intro-component
  [users*]
  (s/setval s/END
            (map get-user-component
                 users*
                 closings)
            [:div {:style {:border (str "0.125em solid " grey)}}
             [:div {:style {:background-color grey
                            :padding          "0.313em"}}
              [:h2 {:style {:display "inline-block"}}
               "Who to follow"]
              [:a {:href     "#"
                   :on-click handle-click
                   :style    {:margin-left "1.25em"}}
               "refresh"]]]))

(def intro
  (aid/<$> intro-component users))

(def endpoint
  "https://api.github.com/users")

(def option
  (aid/<$> (partial assoc-in
                    {:handler (comp response
                                    walk/keywordize-keys)}
                    [:params :since])
           beginning))

(frp/on (partial GET endpoint) option)
