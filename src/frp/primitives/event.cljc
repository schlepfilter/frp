;event and behavior namespaces are separated to limit the impact of :refer-clojure :exclude for transduce
(ns frp.primitives.event
  (:refer-clojure :exclude [transduce])
  (:require [aid.core :as aid :include-macros true]
            [cats.context :as ctx]
            [cats.core :as m]
            [cats.monad.maybe :as maybe]
            [cats.protocols :as protocols]
            [cats.util :as util]
            [com.rpl.specter :as s :include-macros true]
            [linked.core :as linked]
            [loom.alg :as alg]
            [loom.graph :as graph]
            #?@(:clj [[chime :as chime]
                      [clj-time.core :as t]
                      [clj-time.periodic :as periodic]])
            #?(:cljs [cljs.reader :as reader])
            [frp.helpers :as helpers :include-macros true]
            [frp.protocols :as entity-protocols]
            [frp.time :as time]
            [frp.tuple :as tuple])
  #?(:clj (:import [clojure.lang IDeref IFn])))

(declare context)

(defn get-initial-network
  []
  {:cancel     aid/nop
   :dependency (graph/digraph)
   :effects    []
   :function   (linked/map)
   :occs       (linked/map)
   :time       time/epoch})

(def network-state
  (atom (get-initial-network)))

(defn get-occs
  [id network]
  (-> network
      :occs
      id))

(defn get-new-time
  [past]
  (let [current (time/now)]
    (if (= past current)
      (recur past)
      current)))

(defn get-times
  []
  (let [past (time/now)]
    [past (get-new-time past)]))

(declare event?)

(aid/defcurried set-occs
                [occs id network]
                (run! #(-> %
                           ((aid/build or
                                       (comp not
                                             event?
                                             tuple/snd)
                                       (comp #{time/epoch (:time network)}
                                             tuple/fst)))
                           assert)
                      occs)
                (s/setval [:occs id s/END] occs network))

(defn modify-network!
  [occ id network]
  ;TODO advance
  (helpers/call-functions
    (->> network
         :dependency
         alg/topsort
         (mapcat (:modifies! network))
         (concat [(partial s/setval* [:modified s/MAP-VALS] false)
                  ;TODO clear cache
                  (partial s/setval* :time (tuple/fst occ))
                  (set-occs [occ] id)
                  (partial s/setval* [:modified id] true)]))
    network))

(def run-effects!
  (aid/build helpers/call-functions
             :effects
             identity))

(def run-network-state-effects!
  (partial swap! network-state run-effects!))

(def garbage-collect
  (partial s/transform*
           [:occs s/MAP-VALS]
           #(->> %
                 (filter (comp (conj (->> %
                                          (take-last 1)
                                          (map tuple/fst)
                                          set)
                                     time/epoch)
                               tuple/fst))
                 vec)))

(def garbage-collect!
  (partial swap! network-state garbage-collect))

(defrecord Event
  [id]
  protocols/Contextual
  (-get-context [_]
    ;If context is inlined, the following error seems to occur.
    ;java.lang.LinkageError: loader (instance of clojure/lang/DynamicClassLoader): attempted duplicate class definition for name: "nodp/helpers/primitives/event/Event"
    context)
  IFn
  ;TODO implement applyTo
  (#?(:clj  invoke
      :cljs -invoke) [_ a]
    ;e stands for an event, and a stands for any as in Push-Pull Functional Reactive Programming.
    (when (:active @network-state)
      (let [[past current] (get-times)]
        ;Not doing garbage collection is visibly slower.
        (garbage-collect!)
        (reset! network-state
                (modify-network! (tuple/tuple past a)
                                 id
                                 @network-state))
        (run-network-state-effects!)
        (->> (partial s/setval* :time current)
             (swap! network-state))
        (run-network-state-effects!))))
  entity-protocols/Entity
  (-get-keyword [_]
    :event)
  IDeref
  (#?(:clj  deref
      :cljs -deref) [_]
    (get-occs id @network-state))
  protocols/Printable
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

(defn get-id-number*
  [ordered-map]
  (aid/casep ordered-map
             empty? 0
             (comp number?
                   parse-last-key)
             (-> ordered-map
                 parse-last-key
                 inc)
             (->> ordered-map
                  get-last-key
                  (dissoc ordered-map)
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
  [id fs network]
  ;TODO add a node to dependency
  (->> network
       (helpers/call-functions
         (concat [(set-occs [] id)]
                 (map ((aid/curry 3 (aid/flip aid/funcall)) id) fs)))
       (reset! network-state))
  (Event. id))

(defn event*
  [fs]
  (event** (get-id @network-state) fs @network-state))

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

(defn make-get-occs-or-latests
  [initial]
  (if initial
    get-occs
    get-latests))

(aid/defcurried modify-<$>
                [f parent-id initial child-id network]
                ;TODO refactor
                (set-occs (mapv (partial m/<$> f)
                                ((make-get-occs-or-latests initial)
                                  parent-id
                                  network))
                          child-id
                          network))

(defn make-call-once
  [id modify!]
  (aid/if-else (comp :modified
                     id)
               modify!))

(defn set-modify
  [id modify! network]
  (s/setval [:modifies! id]
            [(make-call-once id modify!)
             (partial s/setval* [:modified id] true)]
            network))

(defn make-set-modify-modify
  [modify*]
  [(fn [id network]
     (set-modify id (modify* false id) network))
   (modify* true)])

(def snth
  (comp (partial apply s/srange)
        (partial repeat 2)))

(defn insert-modify
  [modify! id network]
  (s/setval [:modifies! id (-> network
                               :modifies!
                               id
                               count
                               (- 2)
                               snth)]
            [(make-call-once id modify!)]
            network))

(aid/defcurried
  insert-merge-sync
  [parent-id child-id network]
  (insert-modify (fn [network*]
                   (set-occs (get-latests parent-id network*)
                             child-id
                             network*))
                 child-id
                 network))

(defn delay-time-occs
  [t occs]
  (map (partial m/<*> (tuple/tuple t identity)) occs))

(aid/defcurried
  delay-sync
  [parent-id child-id network]
  (set-occs (->> network
                 (get-occs parent-id)
                 (delay-time-occs (:time network)))
            child-id
            network))

(aid/defcurried modify-join
                [parent-id initial child-id network]
                (helpers/call-functions
                  (map (comp (fn [parent-id*]
                               (partial helpers/call-functions
                                        ((juxt add-edge
                                               insert-merge-sync
                                               delay-sync)
                                          parent-id*
                                          child-id)))
                             :id
                             tuple/snd)
                       ((make-get-occs-or-latests initial) parent-id network))
                  network))

(defn merge-one
  [parent merged]
  (s/setval s/END [(first parent)] merged))

(def get-first-time-number
  (comp deref
        tuple/fst
        first))

(defn merge-occs*
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
  (partial merge-occs* []))

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
  (helpers/reify-monad
    (fn [f fa]
      (->> fa
           :id
           (modify-<$> f)
           make-set-modify-modify
           (cons (add-edge (:id fa)))
           event*))
    pure
    #(->> (modify-join (:id %))
          make-set-modify-modify
          (cons (add-edge (:id %)))
          event*)
    protocols/Semigroup
    ;TODO possibly make mappend multi-arity
    (-mappend [_ left-event right-event]
              (-> (modify-<> (:id left-event)
                             (:id right-event))
                  make-set-modify-modify
                  (concat (map (comp add-edge
                                     :id)
                               [left-event right-event]))
                  event*))
    ;TODO delete Monoid
    protocols/Monoid
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
                [f init id network reduction element]
                (s/setval s/END
                          reduction
                          [((aid/lift-a f)
                             (get-transduction init
                                               (get-occs id network)
                                               reduction)
                             element)]))

(defn make-modify-transduce
  [xform]
  ;TODO refactor
  (let [step! (xform (comp maybe/just
                           second
                           vector))]
    (aid/curriedfn [f init parent-id initial child-id network]
                   (-> (get-accumulator f init child-id network)
                       (reduce []
                               (get-elements step!
                                             parent-id
                                             initial
                                             network))
                       (set-occs child-id network)))))

(defn transduce
  ([xform f e]
   (transduce xform f (f) e))
  ([xform f init e]
    ;TODO refactor
   (->> ((make-modify-transduce xform) f init (:id e))
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
    (run-effects! @network-state)))

(defn activate
  ([]
   (activate #?(:clj  Double/POSITIVE_INFINITY
                :cljs js/Number.POSITIVE_INFINITY)))
  ([rate]
   (swap! network-state
          (partial s/setval*
                   :cancel
                   (if (= rate #?(:clj  Double/POSITIVE_INFINITY
                                  :cljs js/Number.POSITIVE_INFINITY))
                     aid/nop
                     #?(:clj  (-> rate
                                  get-periods
                                  (chime/chime-at handle))
                        :cljs (->> (js/setInterval handle rate)
                                   (partial js/clearInterval))))))
   (swap! network-state (partial s/setval* :active true))
   (run-network-state-effects!)
   (time/start)
   (->> (time/now)
        get-new-time
        (partial s/setval* :time)
        (swap! network-state))
   (run-network-state-effects!)))
