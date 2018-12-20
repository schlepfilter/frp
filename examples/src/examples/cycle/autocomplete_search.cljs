(ns examples.cycle.autocomplete-search
  (:require [aid.core :as aid]
            [cats.core :as m]
            [com.rpl.specter :as s]
            [frp.ajax :refer [GET]]
            [frp.clojure.core :as core]
            [frp.core :as frp]
            [examples.helpers :as helpers]))

(frp/defe typing key-down)

(def endpoint
  "https://en.wikipedia.org/w/api.php")

(def response
  (->> typing
       (core/remove empty?)
       (m/=<< (comp (partial GET endpoint)
                    (partial assoc-in
                             {:params {:action "opensearch"
                                       ;https://www.mediawiki.org/wiki/Manual:CORS#Description
                                       ;For anonymous requests, origin query string parameter can be set to * which will allow requests from anywhere.
                                       :origin "*"}}
                             [:params :search])))))

(def enter
  (core/filter (partial = "Enter") key-down))

(def suggested
  (m/<> (aid/<$ true response) (aid/<$ false enter)))

(def relative-number
  (->> (m/<> (->> key-down
                  (core/filter (partial = "ArrowDown"))
                  (aid/<$ inc))
             (->> key-down
                  (core/filter (partial = "ArrowUp"))
                  (aid/<$ dec))
             (aid/<$ (constantly 0) response))
       (frp/accum 0)))

(def suggestions
  (->> response
       (m/<$> second)
       (frp/stepper [])))

(def valid-number
  ((aid/lift-a (fn [relative-number* total-number]
                 (aid/if-else zero?
                              (partial mod relative-number*)
                              total-number)))
    (frp/stepper 0 relative-number)
    (m/<$> count suggestions)))

(def completion
  (->> valid-number
       ((aid/lift-a nth) suggestions)
       (frp/snapshot enter)
       (m/<$> second)))

(defn query-input-component
  [query*]
  [:input {:on-change   #(-> %
                             .-target.value
                             typing)
           :on-key-down #(-> %
                             .-key
                             key-down)
           :type        "text"
           :style       {:width "100%"}
           :value       query*}])

(def label-style
  {:display    "inline-block"
   :text-align "right"
   :width      100})

(def section-style
  {:margin-bottom 10})

(defn autocomplete-search-component
  ;TODO display suggestions
  [query-input* suggestion-list*]
  [:div {:style {:background (helpers/get-color 0 0 0.94)
                 :padding    5}}
   [:section {:style section-style}
    [:label {:style label-style}
     "Query:"]
    [:div {:style {:display  "inline-block"
                   :position "relative"
                   :width    300}}
     query-input*
     suggestion-list*]]
   [:section {:style section-style}
    [:label {:style label-style}
     "Some field:"]
    [:input {:type "text"}]]])

(def query
  (->> (m/<> typing completion)
       (frp/stepper "")))

(def query-input
  (m/<$> query-input-component query))

(def green
  (helpers/get-color (/ 29 72) 0.66 0.74))

(def border
  (str "1px solid " (helpers/get-color 0 0 0.8)))

(defn suggestion-list-component
  [suggested* suggestions* number*]
  (->> suggestions*
       (map-indexed (fn [index x]
                      [:li {:on-click       #(completion x)
                            :on-mouse-enter #(relative-number index)
                            :style          {:border-bottom border
                                             :list-style    "none"
                                             :padding       "3px 0px 3px 8px"}}
                       x]))
       (aid/if-else empty?
                    (partial s/transform*
                             [(s/srange number* (inc number*)) s/ALL]
                             (fn [[_ m s]]
                               [:li
                                (s/setval [:style :background] green m)
                                s])))
       (concat [:ul
                {:style    {:background    "white"
                            :border        border
                            :border-bottom "0px"
                            :box-shadow    (->> (helpers/get-color 0 0 0.863)
                                                (str "0px 4px 4px "))
                            :display       (if suggested*
                                             "block"
                                             "none")
                            :margin        0
                            :padding       0
                            :position      "absolute"
                            :width         "100%"}
                 :on-click #(suggested false)}])
       vec))

(def suggestion-list
  ((aid/lift-a suggestion-list-component)
    (frp/stepper false suggested)
    suggestions
    valid-number))

(def autocomplete-search
  ((aid/lift-a autocomplete-search-component) query-input suggestion-list))
