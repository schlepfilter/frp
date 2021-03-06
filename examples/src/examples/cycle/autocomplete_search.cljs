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
  (->> response
       (aid/<$ (constantly 0))
       (m/<> (->> key-down
                  (core/filter (partial = "ArrowDown"))
                  (aid/<$ inc))
             (->> key-down
                  (core/filter (partial = "ArrowUp"))
                  (aid/<$ dec)))
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
       ((aid/lift-a (comp (aid/if-then-else (comp empty?
                                                  first)
                                            (constantly "")
                                            (partial apply nth))
                          vector))
         suggestions)
       (frp/snapshot enter)
       (m/<$> second)))

(def query
  (->> (m/<> typing completion)
       (frp/stepper "")))

(defn query-component
  [query*]
  [:input {:on-change   #(-> %
                             .-target.value
                             typing)
           :on-key-down #(-> %
                             .-key
                             key-down)
           :style       {:width "100%"}
           :value       query*}])

(def green
  (helpers/get-color (/ 29 72) 0.66 0.74))

(def border
  (->> 0.8
       helpers/get-grey
       (str "1px solid ")))

(defn suggestions-component
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
                               [:li (s/setval [:style :background] green m)
                                s])))
       (concat [:ul {:style    {:border        border
                                :border-bottom "0px"
                                :box-shadow    (->> 0.863
                                                    helpers/get-grey
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

(def label-style
  {:display    "inline-block"
   :text-align "right"
   :width      100})

(def section-style
  {:margin-bottom 10})

(defn autocomplete-search-component
  [query-input* suggestion-list*]
  [:div {:style {:background (helpers/get-grey 0.94)
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

(def autocomplete-search
  ((aid/lift-a autocomplete-search-component)
    (m/<$> query-component query)
    ((aid/lift-a suggestions-component)
      (frp/stepper false suggested)
      suggestions
      valid-number)))
