;event and behavior namespaces are separated to limit the impact of :refer-clojure :exclude for transduce
(ns ^:figwheel-always frp.primitives.event
  (:refer-clojure :exclude [transduce])
  (:require [cljs.analyzer.api :as ana-api]
            #?(:cljs [goog.object :as object])
            #?(:cljs [cljs.reader :as reader])
            [aid.core :as aid :include-macros true]
            [aid.unit :as unit]
            [cats.context :as ctx]
            [cats.core :as m]
            [cats.monad.maybe :as maybe]
            [cats.protocols :as cats-protocols]
            [cats.util :as util]
            [com.rpl.specter :as s :include-macros true]
            [linked.core :as linked]
            [loom.alg :as alg]
            [loom.graph :as graph]
            #?@(:clj [[chime :as chime]
                      [clj-time.core :as t]
                      [clj-time.periodic :as periodic]])
            [frp.helpers :as helpers :include-macros true]
            [frp.protocols :as entity-protocols]
            [frp.time :as time]
            [frp.tuple :as tuple]
            [clojure.set :as set])
  #?(:cljs (:require-macros [frp.primitives.event :refer [get-id-alias*]]))
  #?(:clj (:import [clojure.lang IDeref IFn])))

(declare context)

(def initial-network
  {:dependency (graph/digraph)
   :function   (linked/map)
   :occs       (linked/map)
   :time       time/epoch})

(def network-state
  (atom initial-network))

(defn get-occs
  [id network]
  (-> network
      :occs
      id))

(def get-new-time
  #(let [current (time/now)]
     (if (= % current)
       (recur %)
       current)))

(def get-times
  #((juxt identity
          get-new-time)
     (time/now)))

(declare event?)

(aid/defcurried set-occs
  [occs id network]
  (run! #(-> %
             ;TODO consider cases where event is inside a collection
             tuple/snd
             ((aid/build or
                         (complement event?)
                         (comp (partial every?
                                        (comp (set [time/epoch
                                                    (:time network)])
                                              tuple/fst))
                               deref)))
             assert)
        occs)
  (s/setval [:occs id s/END] occs network))

(def call-functions
  (aid/flip (partial reduce (aid/flip aid/funcall))))

(def call-functions!
  #(call-functions (interleave % (repeat (partial reset! network-state)))
                   @network-state))

(defn modify-network!
  [occ id network]
  ;TODO advance
  ;TODO call modifications only of the events connected to the event with id
  (->> network
       :dependency
       alg/topsort
       (mapcat (:modifications network))
       (concat [(partial s/setval* [:modified s/MAP-VALS] false)
                ;TODO clear cache
                (partial s/setval* :time (tuple/fst occ))
                (set-occs [occ] id)
                (partial s/setval* [:modified id] true)])
       call-functions!))

(def run-effects!
  (comp call-functions!
        :effects))

(def run-network-state-effects!
  (partial swap! network-state run-effects!))

(def garbage-collect
  ;TODO garbage collect in set-occs
  (partial s/transform*
           [:occs s/MAP-VALS]
           ;TODO starting from the leaves of the dependency recursively delete events that have past occurrences and do not have any children or effects
           #(filter (comp (conj (aid/casep %
                                           empty? #{}
                                           #{(-> %
                                                 last
                                                 tuple/fst)})
                                time/epoch)
                          tuple/fst)
                    %)))

(def garbage-collect!
  (partial swap! network-state garbage-collect))

#?(:clj (defmacro get-id-alias*
          []
          (vec (map (fn [x]
                      `(try (do ~x
                                [~(str x)
                                 ~x])
                            (catch js/Error _# {})))
                    (ana-api/all-ns)))))

#?(:cljs
   (when goog/DEBUG
     (def get-id-alias
       #(->> (get-id-alias*)
             (remove (comp nil?
                           second))
             (mapcat (fn [[s x]]
                       (map (fn [k v]
                              [(keyword (str s "/" k)) v])
                            (object/getKeys x)
                            (object/getValues x))))
             (filter (comp event?
                           last))
             (into {})
             (s/transform s/MAP-VALS :id)))

     (def memoized-get-id-alias
       (memoize get-id-alias))

     (def initial-invocations
       [])

     (defonce invocations-state
       (atom initial-invocations))))

(defn invoke**
  [id a]
  (let [[past current] (get-times)]
    ;Doing garbage collection is visibly faster.
    (garbage-collect!)
    (modify-network! (tuple/tuple past a) id @network-state)
    (run-network-state-effects!)
    (swap! network-state (partial s/setval* :time current))
    (run-network-state-effects!)))

(defn invoke*
  [id a]
  (when (:active @network-state)
    #?(:cljs
       (if (and goog/DEBUG
                ;Doing memoization is visibly faster.
                ((memoized-get-id-alias) id))
         (swap! invocations-state (partial s/setval*
                                           s/AFTER-ELEM
                                           [((memoized-get-id-alias) id) a]))))
    (invoke** id a)))

(defrecord Event
  [id]
  cats-protocols/Contextual
  (-get-context [_]
    ;If context is inlined, the following error seems to occur.
    ;java.lang.LinkageError: loader (instance of clojure/lang/DynamicClassLoader): attempted duplicate class definition for name: "nodp/helpers/primitives/event/Event"
    context)
  IFn
  ;TODO implement applyTo
  (#?(:clj  invoke
      :cljs -invoke) [_]
    (invoke* id unit/unit))
  (#?(:clj  invoke
      :cljs -invoke) [_ a]
    (invoke* id a))
  entity-protocols/Entity
  (-get-keyword [_]
    :event)
  IDeref
  (#?(:clj  deref
      :cljs -deref) [_]
    (get-occs id @network-state))
  cats-protocols/Printable
  (-repr [_]
    (str "#[event " id "]")))

(util/make-printable Event)

(def event?
  (partial instance? Event))

(def parse-keyword
  (comp #?(:clj  read-string
           :cljs reader/read-string)
        (partial (aid/flip subs) 1)
        str))

(def get-last-key
  (comp key
        last))

(def parse-last-key
  (comp parse-keyword
        get-last-key))

(def get-id-number*
  #(aid/casep %
              empty? 0
              (comp number?
                    parse-last-key)
              (-> %
                  parse-last-key
                  inc)
              (->> %
                   get-last-key
                   (dissoc %)
                   recur)))

(aid/defcurried get-id-number
  [k network]
  (-> network
      k
      get-id-number*))

(def get-id
  (aid/build (comp keyword
                   str
                   max)
             (get-id-number :occs)
             (get-id-number :function)))

(defn event**
  [id fs]
  ;TODO add a node to dependency
  (->> fs
       (map ((aid/curry 3 (aid/flip aid/funcall)) id))
       (cons (set-occs [] id))
       call-functions!)
  (Event. id))

(def event*
  #(event** (get-id @network-state) %))

(def get-unit
  (partial tuple/tuple time/epoch))

(aid/defcurried add-edge
  [parent-id child-id network]
  (s/transform :dependency
               (partial (aid/flip graph/add-edges)
                        [parent-id child-id])
               network))

(defn get-latests
  [id network]
  (->> network
       (get-occs id)
       (filter (comp (partial = (:time network))
                     tuple/fst))))

(def make-get-occs-or-latests
  #(if %
     get-occs
     get-latests))

(aid/defcurried modify-<$>
  [f! parent-id initial child-id network]
  ;TODO refactor
  (set-occs (->> network
                 ((make-get-occs-or-latests initial) parent-id)
                 (mapv (partial m/<$> f!)))
            child-id
            @network-state))

(defn make-call-once
  [id modify!]
  (aid/if-else (comp id
                     :modified)
               modify!))

(defn set-modify
  [id modify! network]
  (s/setval [:modifications id]
            [(make-call-once id modify!)
             (partial s/setval* [:modified id] true)]
            network))

(defn make-set-modify-modify
  [modify!]
  [(fn [id network]
     (set-modify id (modify! false id) network))
   (modify! true)])

(def snth
  (comp (partial apply s/srange)
        (partial repeat 2)))

(defn insert-modify
  [modify! id network]
  (s/setval [:modifications id (-> network
                                   :modifications
                                   id
                                   count
                                   (- 2)
                                   snth)]
            [(make-call-once id modify!)]
            network))

(aid/defcurried insert-merge-sync
  [parent-id child-id network]
  (insert-modify #(set-occs (get-latests parent-id %) child-id %)
                 child-id
                 network))

(defn delay-time-occs
  [t occs]
  (map (partial m/<*> (tuple/tuple t identity)) occs))

(aid/defcurried delay-sync
  [parent-id child-id network]
  (set-occs (->> network
                 (get-occs parent-id)
                 (delay-time-occs (:time network)))
            child-id
            network))

(aid/defcurried modify-join
  [parent-id initial child-id network]
  (->> network
       ((make-get-occs-or-latests initial) parent-id)
       (map (comp (aid/curriedfn [parent-id* _]
                                 (call-functions! ((juxt add-edge
                                                         insert-merge-sync
                                                         delay-sync)
                                                    parent-id*
                                                    child-id)))
                  :id
                  tuple/snd))
       call-functions!))


(defn merge-one
  [parent merged]
  (s/setval s/AFTER-ELEM (first parent) merged))

(aid/defcurried merge-occs*
  [merged left right]
  (cond (empty? left) (s/setval s/END right merged)
        (empty? right) (s/setval s/END left merged)
        (->> [left right]
             (map (comp tuple/fst
                        first))
             (apply helpers/<=))
        (recur (merge-one left merged) (rest left) right)
        :else
        (recur (merge-one right merged) left (rest right))))

(def merge-occs
  (merge-occs* []))

(aid/defcurried modify-<>
  [left-id right-id initial child-id network]
  (set-occs (merge-occs ((make-get-occs-or-latests initial)
                          left-id
                          network)
                        ((make-get-occs-or-latests initial)
                          right-id
                          network))
            child-id
            network))

(def pure
  (comp event*
        vector
        set-occs
        vector
        get-unit))

(def mempty
  #(event* []))

(def context
  (helpers/reify-monad (fn [f! fa]
                         (->> fa
                              :id
                              (modify-<$> f!)
                              make-set-modify-modify
                              (cons (add-edge (:id fa)))
                              event*))
                       pure
                       #(->> (modify-join (:id %))
                             make-set-modify-modify
                             (cons (add-edge (:id %)))
                             event*)
                       cats-protocols/Semigroup
                       (-mappend [_ left-event right-event]
                                 (-> (modify-<> (:id left-event)
                                                (:id right-event))
                                     make-set-modify-modify
                                     (concat (map (comp add-edge
                                                        :id)
                                                  [left-event right-event]))
                                     event*))
                       ;TODO delete Monoid
                       cats-protocols/Monoid
                       (-mempty [_]
                                (mempty))))

(defn get-elements
  [step! id initial network]
  (->> network
       ((make-get-occs-or-latests initial) id)
       (map (partial s/transform* :snd (comp unreduced
                                             (partial step! aid/nothing))))
       (filter (comp maybe/just?
                     tuple/snd))
       (map (partial m/<$> deref))))

(defn get-transduction
  [init occs reduction]
  (-> (get-unit init)
      vector
      (concat occs reduction)
      last))

(aid/defcurried get-accumulator
  [f! init id network reduction element]
  (cons ((aid/lift-a f!)
          (get-transduction init
                            (get-occs id network)
                            reduction)
          element)
        reduction))

(def make-modify-transduce
  ;TODO refactor
  #(let [step! (% (comp maybe/just
                        second
                        vector))]
     (aid/curriedfn [f! init parent-id initial child-id network]
                    (set-occs (reduce (get-accumulator f! init child-id network)
                                      []
                                      (get-elements step!
                                                    parent-id
                                                    initial
                                                    network))
                              child-id
                              @network-state))))

(defn transduce
  ([xform f e]
   (transduce xform f (f) e))
  ([xform f init e]
    ;TODO refactor
    ;TODO consider cases where f has side effects
   (->> e
        :id
        ((make-modify-transduce xform) f init)
        make-set-modify-modify
        (cons (add-edge (:id e)))
        event*)))

(defn snapshot
  [e b]
  (m/<$> (fn [x]
           [x @b])
         e))

#?(:clj (defn get-periods
          ;TODO extract a purely functional function
          [rate]
          (->> rate
               t/millis
               (periodic/periodic-seq (t/now))
               rest)))

(defn handle
  [_]
  (when (:active @network-state)
    (->> (time/now)
         get-new-time
         (partial s/setval* :time)
         (swap! network-state))
    (run-network-state-effects!)))

(def append-cancellation
  (aid/curry 2 (partial s/setval* [:cancellations s/AFTER-ELEM])))

(defn activate
  ([]
   (activate #?(:clj  Double/POSITIVE_INFINITY
                :cljs js/Number.POSITIVE_INFINITY)))
  ([rate]
   (->> (aid/case-eval rate
                       #?(:clj  Double/POSITIVE_INFINITY
                          :cljs js/Number.POSITIVE_INFINITY)
                       aid/nop
                       #?(:clj  (-> rate
                                    get-periods
                                    (chime/chime-at handle))
                          :cljs (->> (js/setInterval handle rate)
                                     (partial js/clearInterval))))
        append-cancellation
        (swap! network-state))
   (swap! network-state (partial s/setval* :active true))
   (run-network-state-effects!)
   (time/start)
   (->> (time/now)
        get-new-time
        (partial s/setval* :time)
        (swap! network-state))
   (run-network-state-effects!)
    #?(:cljs
       (when goog/DEBUG
         (run! (comp (partial apply invoke**)
                     (partial s/transform*
                              s/FIRST
                              (set/map-invert (memoized-get-id-alias))))
               @invocations-state)))))
