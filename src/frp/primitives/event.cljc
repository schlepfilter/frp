;event and behavior namespaces are separated to limit the impact of :refer-clojure :exclude for transduce
(ns ^:figwheel-always frp.primitives.event
  (:refer-clojure :exclude [transduce])
  (:require [cljs.analyzer.api :as ana-api]
            [clojure.set :as set]
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
            [loom.alg :as alg]
            [loom.graph :as graph]
            [frp.helpers :as helpers]
            [frp.primitives.net :as net]
            [frp.protocols :as entity-protocols]
            [frp.time :as time]
            [frp.tuple :as tuple])
  #?(:cljs (:require-macros [frp.primitives.event :refer [get-namespaces]]))
  #?(:clj (:import [clojure.lang IDeref IFn])))

(declare get-context)

(defn get-occs
  [entity-id net]
  (-> net
      :occs
      entity-id))

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
  [occs entity-id net]
  (s/transform [:occs entity-id]
               (comp (partial s/setval* s/END occs)
                     ;Doing garbage collection is visibly faster.
                     garbage-collect)
               net))

(defn modify-net!
  [occ net-id entity-id net]
  ;TODO advance
  (->> net
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
       ;{:entity-id :invoke, :n-calls 5, :min "216ms", :max "349ms", :mad "39.04ms", :mean "251.4ms", :time% 39, :time "1.26s "}
       ;
       ;Clock Time: (100%) 3.25s
       ;Accounted Time: (39%) 1.26s
       ;nil
       ;
       ;If a subgraph is not taken, it's slower.
       ;
       ;{:entity-id :invoke, :n-calls 5, :min "347ms", :max "578ms", :mad "69.44ms", :mean "414.2ms", :time% 54, :time "2.07s "}
       ;
       ;Clock Time: (100%) 3.85s
       ;Accounted Time: (54%) 2.07s
       ;nil
       ((aid/build graph/subgraph
                   identity
                   (partial (aid/flip alg/bf-traverse) entity-id)))
       alg/topsort
       (mapcat (:modifications net))
       (concat [(partial s/setval* [:modified s/MAP-VALS] false)
                (partial s/setval* :time (tuple/fst occ))
                (set-occs [occ] entity-id)
                (partial s/setval* [:modified entity-id] true)])
       (net/call-functions! net-id)))

(def initial-reloading
  {})

(defonce reloading-state
  (atom initial-reloading))

(aid/defcurried invoke**
  [net-id entity-id a]
  (->> @net/universe-state
       net-id
       (modify-net! (-> (time/now)
                        net/get-new-time
                        (tuple/tuple a))
                    net-id
                    entity-id))
  (net/run-effects-twice! net-id))

(def debugging
  #?(:clj  false
     :cljs goog/DEBUG))

(aid/defcurried invoke*
  [net-id entity-id a]
  (when (-> @net/universe-state
            net-id
            :active)
    (if (-> @net/universe-state
            net-id
            ((aid/build or
                        :effective
                        (comp (partial = time/epoch)
                              :time))))
      (swap! net/universe-state
             (partial s/setval*
                      [net-id :invocations s/AFTER-ELEM]
                      #(invoke* net-id entity-id a)))
      ;TODO make debugging compatible with multiple nets
      (do (if debugging
            (swap! reloading-state
                   (partial s/setval*
                            [:entity-id-invocations s/AFTER-ELEM]
                            [entity-id a])))
          (invoke** net-id entity-id a)))))

(defrecord Event
  [net-id entity-id]
  cats-protocols/Contextual
  (-get-context [_]
    ;If context is inlined, the following error seems to occur.
    ;java.lang.LinkageError: loader (instance of clojure/lang/DynamicClassLoader): attempted duplicate class definition for name: "nodp/helpers/primitives/event/Event"
    (get-context net-id))
  IFn
  ;TODO implement applyTo
  (#?(:clj  invoke
      :cljs -invoke) [_]
    (invoke* net-id entity-id unit/unit))
  (#?(:clj  invoke
      :cljs -invoke) [_ a]
    (invoke* net-id entity-id a))
  entity-protocols/Entity
  (-get-keyword [_]
    :event)
  IDeref
  (#?(:clj  deref
      :cljs -deref) [_]
    (->> @net/universe-state
         net-id
         (get-occs entity-id)))
  cats-protocols/Printable
  (-repr [_]
    (str "#[event " net-id " " entity-id "]")))

(util/make-printable Event)

(def event?
  (partial instance? Event))

(defn event**
  [net-id entity-id fs]
  ;TODO add a node to dependency
  (->> fs
       (map ((aid/curry 3 (aid/flip aid/funcall)) entity-id))
       (cons (set-occs [] entity-id))
       (net/call-functions! net-id))
  (Event. net-id entity-id))

(aid/defcurried event*
  [net-id fs]
  (event** net-id
           (->> @net/universe-state
                net-id
                :occs
                net/get-id)
           fs))

(def get-unit
  (partial tuple/tuple time/epoch))

(aid/defcurried add-edge
  [parent-id child-id net]
  (s/transform :dependency
               (partial (aid/flip graph/add-edges)
                        [parent-id child-id])
               net))

(defn get-latests
  [entity-id net]
  (->> net
       (get-occs entity-id)
       (filter (comp (partial = (:time net))
                     tuple/fst))))

(defn get-occs-or-latests
  [initial entity-id net]
  ((if initial
     get-occs
     get-latests)
    entity-id
    net))

(aid/defcurried modify-<$>
  [f! net-id parent-id initial child-id net]
  ;TODO refactor
  (set-occs (net/with-net (net/->Net net-id)
                          (->> net
                               (get-occs-or-latests initial parent-id)
                               (mapv (partial m/<$> f!))))
            child-id
            (net-id @net/universe-state)))

(defn make-call-once
  [entity-id modify!]
  (aid/if-else (comp entity-id
                     :modified)
               modify!))

(defn set-modification
  [entity-id modify! net]
  (s/setval [:modifications entity-id]
            [(make-call-once entity-id modify!)
             (partial s/setval* [:modified entity-id] true)]
            net))

(defn make-set-modification-modification
  [modify!]
  [(fn [entity-id net]
     (set-modification entity-id (modify! false entity-id) net))
   (modify! true)])

(def snth
  (comp (partial apply s/srange)
        (partial repeat 2)))

(defn insert-modification
  [modify! entity-id net]
  (s/setval [:modifications entity-id (-> net
                                          :modifications
                                          entity-id
                                          count
                                          (- 2)
                                          snth)]
            [(make-call-once entity-id modify!)]
            net))

(aid/defcurried insert-merge-sync
  [parent-id child-id net]
  (insert-modification #(set-occs (get-latests parent-id %) child-id %)
                       child-id
                       net))

(defn delay-time-occs
  [t occs]
  (map (partial m/<*> (tuple/tuple t identity)) occs))

(aid/defcurried delay-sync
  [parent-id child-id net]
  (->> net
       (get-occs parent-id)
       (run! #(->> %
                   tuple/fst
                   ((set [time/epoch (:time net)]))
                   assert)))
  (set-occs (->> net
                 (get-occs parent-id)
                 (delay-time-occs (:time net)))
            child-id
            net))

(aid/defcurried modify-join
  [net-id parent-id initial child-id net]
  (->> net
       (get-occs-or-latests initial parent-id)
       (map (comp (aid/curriedfn [parent-id* _]
                                 (net/call-functions! net-id
                                                      ((juxt add-edge
                                                             insert-merge-sync
                                                             delay-sync)
                                                        parent-id*
                                                        child-id)))
                  :entity-id
                  tuple/snd))
       (net/call-functions! net-id)))

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
  [left-id right-id initial child-id net]
  (set-occs (merge-occs (get-occs-or-latests initial left-id net)
                        (get-occs-or-latests initial right-id net))
            child-id
            net))

(defn pure*
  [net-id a]
  (->> a
       get-unit
       vector
       set-occs
       vector
       (event* net-id)))

(def pure
  #(pure* net/*net-id* %))

(def mempty*
  (partial (aid/flip event*) []))

(def mempty
  #(mempty* net/*net-id*))

(defn get-context
  [net-id]
  (reify
    entity-protocols/Entity
    (-get-net-id [_]
      net-id)
    cats-protocols/Context
    cats-protocols/Functor
    (-fmap [_ f! fa]
      (->> fa
           ((aid/build (modify-<$> f!)
                       :net-id
                       :entity-id))
           make-set-modification-modification
           (cons (add-edge (:entity-id fa)))
           (event* (:net-id fa))))
    cats-protocols/Applicative
    (-pure [context a]
      (pure* (entity-protocols/-get-net-id context) a))
    (-fapply [_ fab fa]
      (aid/ap fab fa))
    cats-protocols/Monad
    (-mreturn [context a]
      (cats-protocols/-pure context a))
    (-mbind [_ ma f!]
      (let [mb (m/<$> f! ma)]
        (->> mb
             ((aid/build modify-join
                         :net-id
                         :entity-id))
             make-set-modification-modification
             (cons (add-edge (:entity-id mb)))
             (event* (:net-id mb)))))
    cats-protocols/Semigroup
    (-mappend [_ left-event right-event]
      (->> [left-event right-event]
           (map (comp add-edge
                      :entity-id))
           (concat
             (make-set-modification-modification
               (modify-<> (:entity-id left-event)
                          (:entity-id right-event))))
           (event* (:net-id left-event))))
    ;TODO delete Monoid
    cats-protocols/Monoid
    (-mempty [context]
      (mempty* (entity-protocols/-get-net-id context)))))

(defn get-elements
  [step! entity-id initial net]
  (->> net
       (get-occs-or-latests initial entity-id)
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
  [f! init entity-id net reduction element]
  (s/setval s/AFTER-ELEM
            ((aid/lift-a f!)
              (get-transduction init
                                (get-occs entity-id net)
                                reduction)
              element)
            reduction))

(def make-modify-transduce
  ;TODO refactor
  #(let [step! (% (comp maybe/just
                        second
                        vector))]
     (aid/curriedfn [f! init net-id parent-id initial child-id net]
                    (set-occs (reduce (get-accumulator f! init child-id net)
                                      []
                                      (get-elements step!
                                                    parent-id
                                                    initial
                                                    net))
                              child-id
                              (net-id @net/universe-state)))))

(defn transduce
  ([xform f e]
   (transduce xform f (f) e))
  ([xform f init e]
    ;TODO refactor
    ;TODO consider cases where f has side effects
   (->> e
        ((aid/build ((make-modify-transduce xform) f init)
                    :net-id
                    :entity-id))
        make-set-modification-modification
        (cons (add-edge (:entity-id e)))
        (event* (:net-id e)))))

(defn snapshot
  [e & bs]
  (m/<$> #(->> bs
               (mapv deref)
               (cons %))
         e))

#?(:clj (defn get-periods
          ;TODO extract a purely functional function
          [rate]
          (->> rate
               t/millis
               (periodic/periodic-seq (t/now))
               rest)))

(def handle
  #(when (-> @net/universe-state
             %
             :active)
     (->> (time/now)
          net/get-new-time
          (partial s/setval* [% :time])
          (swap! net/universe-state))
     (net/run-effects-twice! %)))

(aid/defcurried append-cancellation
  [net-id f! universe]
  (s/setval [net-id :cancellations s/AFTER-ELEM] f! universe))

(def positive-infinity
  #?(:clj  Double/POSITIVE_INFINITY
     :cljs js/Number.POSITIVE_INFINITY))

(def run-universe-effects!
  #(->> @net/universe-state
        keys
        (run! %)))

(defn run-effects-once!
  [net-id]
  (net/set-effective net-id true)
  (net/run-effects!* net-id)
  (net/set-effective net-id false))

(defn activate*
  [rate]
  (swap!
    net/universe-state
    (fn [universe]
      (->>
        universe
        keys
        (reduce
          (fn [reduction element]
            (append-cancellation element
                                 (aid/case-eval rate
                                   positive-infinity aid/nop
                                   #?(:clj
                                      (chime/chime-at (get-periods rate)
                                                      (fn [_]
                                                        (handle element)))
                                      :cljs
                                      (partial js/clearInterval
                                               (js/setInterval (partial handle
                                                                        element)
                                                               rate))))
                                 reduction))
          universe))))
  (swap! net/universe-state (partial s/setval* [s/MAP-VALS :active] true))
  (run-universe-effects! run-effects-once!)
  (time/start)
  (swap! net/universe-state
         (partial s/setval* [s/MAP-VALS :time] (net/get-new-time (time/now))))
  (run-universe-effects! net/run-effects-twice!))

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
                                                  (invoke** net/initial-net-id))
                                         (partial s/transform*
                                                  s/FIRST
                                                  alias-id)))
                          :alias-invocations))
            (partial s/setval* :entity-id-invocations [])
            (partial s/setval* :entity-id alias-id)
            #(s/setval [:alias-invocations s/END]
                       (->> %
                            :entity-id-invocations
                            (filter (comp (-> %
                                              :entity-id
                                              set/map-invert)
                                          first))
                            (s/transform [s/ALL s/FIRST]
                                         (-> %
                                             :entity-id
                                             set/map-invert)))
                       %)))))

#?(:cljs (def get-alias-id
           (comp (partial apply hash-map)
                 (partial mapcat (juxt (comp keyword
                                             (partial (aid/flip subs) 2)
                                             str)
                                       (comp :entity-id
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
