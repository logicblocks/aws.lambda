(ns aws.lambda.adapters.test-support.clock
  (:require
    [aws.lambda.adapters.clock :as clock]))

(defrecord FixedClock [time-in-millis]
  clock/Clock
  (millis [_]
    time-in-millis))

(defn fixed-clock [time-in-millis]
  (->FixedClock time-in-millis))
