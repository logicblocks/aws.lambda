(ns aws.lambda.adapters.clock)

(defprotocol Clock
  (millis [_]))

(defrecord SystemClock []
  Clock
  (millis [_]
    (System/currentTimeMillis)))

(defn system-clock []
  (->SystemClock))
