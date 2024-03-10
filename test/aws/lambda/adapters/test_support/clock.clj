(ns aws.lambda.adapters.test-support.clock
  (:require
   [cljc.java-time.clock :as jtc]
   [cljc.java-time.instant :as jti]
   [tick.core :as tc]))

(defn fixed-clock [time-in-millis]
  (jtc/fixed
    (jti/of-epoch-milli time-in-millis)
    (tc/current-zone)))
