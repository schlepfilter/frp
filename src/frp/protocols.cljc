(ns frp.protocols)

(defprotocol Entity
  (-get-keyword [_])
  (-get-network-id [_]))
