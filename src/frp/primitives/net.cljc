(ns frp.primitives.net
  (:require #?(:cljs [cljs.reader :as reader])
            [aid.core :as aid]
            [cats.protocols :as cats-protocols]
            [cats.util :as util]
            [com.rpl.specter :as s]
            [linked.core :as linked]
            [loom.graph :as graph]
            [frp.time :as time])
  #?(:clj (:import [clojure.lang IDeref IFn]))
  #?(:cljs (:require-macros frp.primitives.net)))

;TODO move net definitions to its own namespace
(def initial-net-id
  :0)

(def initial-net
  {:dependency (graph/digraph)
   :function   (linked/map)
   :occs       (linked/map)
   :time       time/epoch})

(def initial-universe
  (linked/map initial-net-id initial-net))

(def universe-state
  (atom initial-universe))

(def call-functions
  (aid/flip (partial reduce (aid/flip aid/funcall))))

(aid/defcurried call-functions!
  [net-id fs]
  (->> @universe-state
       net-id
       (call-functions (->> (comp net-id
                                  (partial swap!
                                           universe-state)
                                  (partial (aid/curry 3
                                                      s/setval*)
                                           net-id))
                            repeat
                            (interleave fs)))))

(defn run-effects!*
  [net-id]
  (->> @universe-state
       net-id
       :effect
       vals
       (call-functions! net-id)))

(defn set-effective
  [net-id x]
  (swap! universe-state (partial s/setval* [net-id :effective] x)))

(defn run-invocations
  [net-id]
  (when-not (-> @universe-state
                net-id
                :invocations
                empty?)
    (let [f! (-> @universe-state
                 net-id
                 :invocations
                 first)]
      (swap! universe-state
             (partial s/transform* [net-id :invocations] rest))
      (f!))
    (recur net-id)))

(def get-new-time
  #(let [current (time/now)]
     (aid/case-eval %
       current (recur %)
       current)))

(defn run-effects-twice!
  [net-id]
  (set-effective net-id true)
  (run-effects!* net-id)
  (swap! universe-state
         (partial s/setval* [net-id :time] (get-new-time (time/now))))
  (run-effects!* net-id)
  (set-effective net-id false)
  (run-invocations net-id))

;TODO rename this function as invoke*
(aid/defcurried invoke-net
  [net-id x]
  (when (-> @universe-state
            net-id
            (not= x))
    (swap! universe-state (comp (partial s/setval* [net-id :cache] s/NONE)
                                (partial s/setval* net-id x)))
    (run-effects-twice! net-id)))

(defrecord Net
  [net-id]
  IFn
  #?(:clj (applyTo [_ xs]
            (run! (invoke-net net-id) xs)))
  (#?(:clj  invoke
      :cljs -invoke) [_ x]
    (invoke-net net-id x))
  IDeref
  (#?(:clj  deref
      :cljs -deref) [_]
    (net-id @universe-state))
  cats-protocols/Printable
  (-repr [_]
    (str "#[net " net-id "]")))

(util/make-printable Net)

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

(def net
  #(let [net-id (get-id @universe-state)]
     (swap! universe-state (partial s/setval* net-id initial-net))
     (->Net net-id)))

(def ^:dynamic *net-id*
  initial-net-id)

(defmacro with-net
  [net expr]
  `(binding [*net-id* (:net-id ~net)]
     ~expr))
