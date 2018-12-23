;event and behavior namespaces are separated to limit the impact of :refer-clojure :exclude for transduce
(ns ^:figwheel-always frp.primitives.event
  (:refer-clojure :exclude [transduce])
  (:require [cljs.analyzer.api :as ana-api]
            #?(:cljs [cljs.reader :as reader])
            [clojure.set :as set]
            #?(:cljs [goog.object :as object])
            [aid.core :as aid]
            [aid.unit :as unit]
            [cats.core :as m]
            [cats.monad.maybe :as maybe]
            [cats.protocols :as cats-protocols]
            [cats.util :as util]
            #?@(:clj [[chime :as chime]
                      [clj-time.core :as t]
                      [clj-time.periodic :as periodic]])
            [com.rpl.specter :as s]
            [linked.core :as linked]
            [loom.alg :as alg]
            [loom.graph :as graph]
            [frp.helpers :as helpers]
            [frp.protocols :as entity-protocols]
            [frp.time :as time]
            [frp.tuple :as tuple])
  #?(:cljs (:require-macros [frp.primitives.event :refer [get-namespaces]]))
  #?(:clj (:import [clojure.lang IDeref IFn])))

(declare context)

;TODO move network definitions to its own namespace
(def initial-network-id
  :0)

(def initial-network
  {:dependency (graph/digraph)
   :function   (linked/map)
   :occs       (linked/map)
   :time       time/epoch})

(def initial-universe
  (linked/map initial-network-id initial-network))

(def universe-state
  (atom initial-universe))

(defn get-occs
  ;TODO rename id as event-id
  [id network]
  (-> network
      :occs
      id))

(def get-new-time
  #(let [current (time/now)]
     (aid/case-eval %
       current (recur %)
       current)))

(def get-times
  ;TODO don't lose a millisecond
  #((juxt identity
          get-new-time)
     (time/now)))

(declare event?)

(def garbage-collect
  #(filter (comp (conj (aid/casep %
                         empty? #{}
                         #{(-> %
                               last
                               tuple/fst)})
                       time/epoch)
                 tuple/fst)
           %))

(aid/defcurried set-occs
  ;TODO rename network as network*
  [occs id network]
  (s/transform [:occs id]
               (comp (partial s/setval* s/END occs)
                     ;Doing garbage collection is visibly faster.
                     garbage-collect)
               network))

(def call-functions
  (aid/flip (partial reduce (aid/flip aid/funcall))))

(aid/defcurried call-functions!
  [network-id fs]
  (->> @universe-state
       network-id
       (call-functions (->> (comp network-id
                                  (partial swap!
                                           universe-state)
                                  (partial (aid/curry 3
                                                      s/setval*)
                                           network-id))
                            repeat
                            (interleave fs)))))

(defn modify-network!
  [occ network-id id network]
  ;TODO advance
  (->> network
       :dependency
       ;Taking a subgraph seems faster.
       ;(tufte/add-basic-println-handler! {})
       ;
       ;(profile {}
       ;         (dotimes [_ 5]
       ;           (let [e (frp/event)]
       ;            (doall (repeatedly 100 #(m/<$> identity frp/event)))
       ;            (frp/activate)
       ;            (p :invoke (run! e (repeat 100 0))))))
       ;
       ;If a subgraph is taken, it's faster.
       ;
       ;{:id :invoke, :n-calls 5, :min "216ms", :max "349ms", :mad "39.04ms", :mean "251.4ms", :time% 39, :time "1.26s "}
       ;
       ;Clock Time: (100%) 3.25s
       ;Accounted Time: (39%) 1.26s
       ;nil
       ;
       ;If a subgraph is not taken, it's slower.
       ;
       ;{:id :invoke, :n-calls 5, :min "347ms", :max "578ms", :mad "69.44ms", :mean "414.2ms", :time% 54, :time "2.07s "}
       ;
       ;Clock Time: (100%) 3.85s
       ;Accounted Time: (54%) 2.07s
       ;nil
       ((aid/build graph/subgraph
                   identity
                   (partial (aid/flip alg/bf-traverse) id)))
       alg/topsort
       (mapcat (:modifications network))
       (concat [(partial s/setval* [:modified s/MAP-VALS] false)
                ;TODO clear cache
                (partial s/setval* :time (tuple/fst occ))
                (set-occs [occ] id)
                (partial s/setval* [:modified id] true)])
       (call-functions! network-id)))

(defn run-effects!
  [network-id]
  (call-functions! network-id
                   (concat [(partial s/setval* [network-id :effective] true)]
                           (:effects (network-id @universe-state))
                           [(partial s/setval* [network-id :effective] false)]))
  (->> @universe-state
       network-id
       :invocations
       (run! (fn [f!]
               (swap! universe-state
                      (partial s/transform* [network-id :invocations] rest))
               (f!)))))

(def initial-reloading
  {})

(defonce reloading-state
  (atom initial-reloading))

(aid/defcurried invoke**
  [network-id id a]
  (let [[past current] (get-times)]
    (->> @universe-state
         network-id
         (modify-network! (tuple/tuple past a) network-id id))
    (run-effects! network-id)
    (swap! universe-state (partial s/setval* [network-id :time] current))
    (run-effects! network-id)))

(def debugging
  #?(:clj  false
     :cljs goog/DEBUG))

(defn invoke*
  [network-id id a]
  (when (-> @universe-state
            network-id
            :active)
    (if (-> @universe-state
            network-id
            ((aid/build or
                        :effective
                        (comp (partial = time/epoch)
                              :time))))
      (swap! universe-state
             (partial s/setval*
                      [network-id :invocations s/AFTER-ELEM]
                      #(invoke* network-id id a)))
      ;TODO make debugging compatible with multiple networks
      (do (if debugging
            (swap! reloading-state
                   (partial s/setval* [:id-invocations s/AFTER-ELEM] [id a])))
          (invoke** network-id id a)))))

(defrecord Event
  [network-id id]
  cats-protocols/Contextual
  (-get-context [_]
    ;If context is inlined, the following error seems to occur.
    ;java.lang.LinkageError: loader (instance of clojure/lang/DynamicClassLoader): attempted duplicate class definition for name: "nodp/helpers/primitives/event/Event"
    context)
  IFn
  ;TODO implement applyTo
  (#?(:clj  invoke
      :cljs -invoke) [_]
    (invoke* network-id id unit/unit))
  (#?(:clj  invoke
      :cljs -invoke) [_ a]
    (invoke* network-id id a))
  entity-protocols/Entity
  (-get-keyword [_]
    :event)
  IDeref
  (#?(:clj  deref
      :cljs -deref) [_]
    (->> @universe-state
         network-id
         (get-occs id)))
  cats-protocols/Printable
  (-repr [_]
    (str "#[event " network-id " " id "]")))

(util/make-printable Event)

(def event?
  (partial instance? Event))

(def parse-keyword
  (comp #?(:clj  read-string
           :cljs reader/read-string)
        name))

(def get-last-key
  (comp key
        last))

(def parse-last-key
  (comp parse-keyword
        get-last-key))

(def get-id-number
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

(def get-id
  ;TODO return uuid for production
  (comp keyword
        str
        get-id-number))

(defn event**
  [network-id id fs]
  ;TODO add a node to dependency
  (->> fs
       (map ((aid/curry 3 (aid/flip aid/funcall)) id))
       (cons (set-occs [] id))
       (call-functions! network-id))
  (Event. network-id id))

(def ^:dynamic *network-id*
  initial-network-id)

(aid/defcurried event*
  [network-id fs]
  (event** network-id
           (->> @universe-state
                network-id
                :occs
                get-id)
           fs))

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

(defn get-occs-or-latests
  [initial id network]
  ((if initial
     get-occs
     get-latests)
    id
    network))

(aid/defcurried modify-<$>
  [f! network-id parent-id initial child-id network]
  ;TODO refactor
  (set-occs (binding [*network-id* network-id]
              (->> network
                   (get-occs-or-latests initial parent-id)
                   (mapv (partial m/<$> f!))))
            child-id
            (network-id @universe-state)))

(defn make-call-once
  [id modify!]
  (aid/if-else (comp id
                     :modified)
               modify!))

(defn set-modification
  [id modify! network]
  (s/setval [:modifications id]
            [(make-call-once id modify!)
             (partial s/setval* [:modified id] true)]
            network))

(defn make-set-modification-modification
  [modify!]
  [(fn [id network]
     (set-modification id (modify! false id) network))
   (modify! true)])

(def snth
  (comp (partial apply s/srange)
        (partial repeat 2)))

(defn insert-modification
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
  (insert-modification #(set-occs (get-latests parent-id %) child-id %)
                       child-id
                       network))

(defn delay-time-occs
  [t occs]
  (map (partial m/<*> (tuple/tuple t identity)) occs))

(aid/defcurried delay-sync
  [parent-id child-id network]
  (->> network
       (get-occs parent-id)
       (run! #(->> %
                   tuple/fst
                   ((set [time/epoch (:time network)]))
                   assert)))
  (set-occs (->> network
                 (get-occs parent-id)
                 (delay-time-occs (:time network)))
            child-id
            network))

(aid/defcurried modify-join
  [network-id parent-id initial child-id network]
  (->> network
       (get-occs-or-latests initial parent-id)
       (map (comp (aid/curriedfn [parent-id* _]
                                 (call-functions! network-id
                                                  ((juxt add-edge
                                                         insert-merge-sync
                                                         delay-sync)
                                                    parent-id*
                                                    child-id)))
                  :id
                  tuple/snd))
       (call-functions! network-id)))

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
  (set-occs (merge-occs (get-occs-or-latests initial left-id network)
                        (get-occs-or-latests initial right-id network))
            child-id
            network))

(def pure
  (comp (event* *network-id*)
        vector
        set-occs
        vector
        get-unit))

(def mempty
  #(event* *network-id* []))

(def context
  (helpers/reify-monad
    (fn [f! fa]
      (->> fa
           ((aid/build (modify-<$> f!)
                       :network-id
                       :id))
           make-set-modification-modification
           (cons (add-edge (:id fa)))
           (event* (:network-id fa))))
    pure
    #(->> %
          ((aid/build modify-join
                      :network-id
                      :id))
          make-set-modification-modification
          (cons (add-edge (:id %)))
          (event* (:network-id %)))
    cats-protocols/Semigroup
    (-mappend [_ left-event right-event]
              (->>
                [left-event right-event]
                (map (comp add-edge
                           :id))
                (concat
                  (make-set-modification-modification
                    (modify-<> (:id left-event)
                               (:id right-event))))
                (event* (:network-id left-event))))
    ;TODO delete Monoid
    cats-protocols/Monoid
    (-mempty [_]
             (mempty))))

(defn get-elements
  [step! id initial network]
  (->> network
       (get-occs-or-latests initial id)
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
  (s/setval s/AFTER-ELEM
            ((aid/lift-a f!)
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
     (aid/curriedfn [f! init network-id parent-id initial child-id network]
                    (set-occs (reduce (get-accumulator f! init child-id network)
                                      []
                                      (get-elements step!
                                                    parent-id
                                                    initial
                                                    network))
                              child-id
                              (network-id @universe-state)))))

(defn transduce
  ([xform f e]
   (transduce xform f (f) e))
  ([xform f init e]
    ;TODO refactor
    ;TODO consider cases where f has side effects
   (->> e
        ((aid/build ((make-modify-transduce xform) f init)
                    :network-id
                    :id))
        make-set-modification-modification
        (cons (add-edge (:id e)))
        (event* (:network-id e)))))

(defn snapshot
  [e b]
  (m/<$> (fn [x]
           [x @b])
         e))

(defrecord Network
  [id]
  IFn
  (#?(:clj  invoke
      :cljs -invoke) [_ x]
    (swap! universe-state (partial s/setval* id x))
    (run-effects! id))
  IDeref
  (#?(:clj  deref
      :cljs -deref) [_]
    (id @universe-state))
  cats-protocols/Printable
  (-repr [_]
    (str "#[network " id "]")))

(util/make-printable Network)

(def network
  #(let [network-id (get-id @universe-state)]
     (swap! universe-state (partial s/setval* network-id initial-network))
     (->Network network-id)))

(defmacro with-network
  [network expr]
  `(binding [*network-id* (:id ~network)]
     ~expr))

#?(:clj (defn get-periods
          ;TODO extract a purely functional function
          [rate]
          (->> rate
               t/millis
               (periodic/periodic-seq (t/now))
               rest)))

(aid/defcurried handle
  [network-id _]
  (when (-> @universe-state
            network-id
            :active)
    (->> (time/now)
         get-new-time
         (partial s/setval* [network-id :time])
         (swap! universe-state))
    (run-effects! network-id)))

(aid/defcurried append-cancellation
  [network-id f! universe]
  (s/setval [network-id :cancellations s/AFTER-ELEM] f! universe))

(def positive-infinity
  #?(:clj  Double/POSITIVE_INFINITY
     :cljs js/Number.POSITIVE_INFINITY))

(def run-universe-effects!
  #(->> @universe-state
        keys
        (run! run-effects!)))

(defn activate*
  [rate]
  (swap!
    universe-state
    (fn [universe]
      (->>
        universe
        keys
        (reduce (fn [reduction element]
                  (append-cancellation element
                                       (aid/case-eval rate
                                         positive-infinity aid/nop
                                         (#?(:clj
                                             (comp chime/chime-at
                                                   (get-periods rate))
                                             :cljs
                                             #(partial js/clearInterval
                                                       (js/setInterval %
                                                                       rate)))
                                           (handle element)))
                                       reduction))
                universe))))
  (swap! universe-state (partial s/setval* [s/MAP-VALS :active] true))
  (run-universe-effects!)
  (time/start)
  (->> (time/now)
       get-new-time
       (partial s/setval* [s/MAP-VALS :time])
       (swap! universe-state))
  (run-universe-effects!))

(aid/defcurried effect
  [f x]
  (f x)
  x)

(defn reload*
  [alias-id]
  (if debugging
    (swap!
      reloading-state
      (comp (effect (comp (partial run!
                                   (comp (partial apply
                                                  (invoke** initial-network-id))
                                         (partial s/transform*
                                                  s/FIRST
                                                  alias-id)))
                          :alias-invocations))
            (partial s/setval* :id-invocations [])
            (partial s/setval* :id alias-id)
            #(s/setval [:alias-invocations s/END]
                       (->> %
                            :id-invocations
                            (filter (comp (-> %
                                              :id
                                              set/map-invert)
                                          first))
                            (s/transform [s/ALL s/FIRST]
                                         (-> %
                                             :id
                                             set/map-invert)))
                       %)))))

#?(:cljs (def get-alias-id
           (comp (partial apply hash-map)
                 (partial mapcat (juxt (comp keyword
                                             (partial (aid/flip subs) 2)
                                             str)
                                       (comp :id
                                             deref)))
                 (partial filter (comp event?
                                       deref))
                 (partial map second)
                 (partial mapcat ns-interns*)
                 (partial filter find-ns)
                 (partial map symbol))))

(def reload
  #?(:clj  aid/nop
     :cljs (comp reload*
                 get-alias-id)))

(defmacro get-namespaces
  []
  (->> (try (ana-api/all-ns)
            #?(:clj (catch NullPointerException _ [])))
       (map str)
       vec))

(defmacro activate
  ([]
   `(activate positive-infinity))
  ([rate]
   `(let [activation# (activate* ~rate)]
      (reload (get-namespaces))
      activation#)))
