(ns aws.lambda.adapters.context-test
  (:require
    [clojure.test :refer [deftest is]]

    [aws.lambda.adapters.context :as context]
    [aws.lambda.adapters.test-support.data :as data]
    [aws.lambda.adapters.test-support.clock :as clock]))

(deftest normalise-context-extracts-top-level-fields-into-map
  (let [aws-request-id (data/random-aws-request-id)
        function-name (data/random-function-name)
        function-version (data/random-function-version)
        memory-limit-in-mb (data/random-memory-amount-in-megabytes)
        log-group-name
        (data/random-log-group-name {:function-name function-name})
        log-stream-name
        (data/random-log-stream-name {:function-version function-version})
        invoked-function-arn
        (data/random-function-arn {:function-name function-name})

        context-options
        {:aws-request-id           aws-request-id
         :function-name            function-name
         :function-version         function-version
         :memory-limit-in-mb       memory-limit-in-mb
         :log-group-name           log-group-name
         :log-stream-name          log-stream-name
         :invoked-function-arn     invoked-function-arn}

        raw-context (data/raw-lambda-context context-options)

        normalised-context (context/normalise-context raw-context)]
    (is (= context-options
          (dissoc
            normalised-context
            :deadline-time-in-millis
            :identity
            :client-context)))))

(deftest normalise-context-calculates-deadline-time-in-millis
  (let [now-in-millis (System/currentTimeMillis)
        clock (clock/fixed-clock now-in-millis)

        remaining-time-in-millis (data/random-millisecond-duration)

        raw-context (data/raw-lambda-context
                      {:remaining-time-in-millis remaining-time-in-millis})

        normalised-context (context/normalise-context raw-context clock)]
    (is (= (+ now-in-millis remaining-time-in-millis)
          (:deadline-time-in-millis normalised-context)))))

(deftest normalise-context-has-nil-identity-when-not-on-raw-context
  (let [raw-context (data/raw-lambda-context
                      {:identity nil})

        normalised-context (context/normalise-context raw-context)]
    (is (nil? (:identity normalised-context)))))

(deftest normalise-context-has-nil-client-context-when-not-on-raw-context
  (let [raw-context (data/raw-lambda-context
                      {:client-context nil})

        normalised-context (context/normalise-context raw-context)]
    (is (nil? (:client-context normalised-context)))))

;; add logger when normalising context
;; test client context and identity normalisation
