(ns frp.protocols)

(defprotocol Entity
  (-get-keyword [_])
  (-get-net-id [_]))
