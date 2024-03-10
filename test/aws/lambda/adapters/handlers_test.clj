(ns aws.lambda.adapters.handlers-test
  (:require
    [clojure.java.io :as io]
    [clojure.test :refer [deftest is]]

    [cartus.test :as ct]

    [aws.lambda.adapters.utils :as utils]
    [aws.lambda.adapters.context :as context]
    [aws.lambda.adapters.handlers :as handlers :refer [def-lambda-handler]]
    [aws.lambda.adapters.test-support.data :as data]
    [aws.lambda.adapters.test-support.clock :as clock])
  (:import
    [java.io ByteArrayOutputStream]))

(defn request-input-stream [event]
  (io/input-stream (.getBytes (utils/->json event))))

(defn response-output-stream []
  (ByteArrayOutputStream.))

(def initialiser-spy (atom nil))

(def-lambda-handler
  {:name "test.handler.InitialiserTest"
   :initialiser
   (fn [] (reset! initialiser-spy true))})

(deftest lambda-handler-calls-provided-initialiser-on-creation
  (import 'test.handler.InitialiserTest)
  (test.handler.InitialiserTest.)
  (is (true? (deref initialiser-spy))))

(def state-spy (atom nil))

(def-lambda-handler
  {:name "test.handler.StateTest"
   :initialiser
   (fn [] {:first 1 :second 2})
   :request-handler
   (fn [state _ _ _]
     (reset! state-spy state))})

(deftest lambda-handler-passes-state-to-request-handler-on-handle
  (import 'test.handler.StateTest)
  (let [lambda-handler (test.handler.StateTest.)]
    (handlers/handle-request lambda-handler
      (request-input-stream {})
      (response-output-stream)
      (data/raw-lambda-context))

    (is (= {:first 1 :second 2} (deref state-spy)))))

(def context-spy (atom nil))
(def now-in-millis (System/currentTimeMillis))

(def-lambda-handler
  {:name  "test.handler.ContextTest"
   :clock (clock/fixed-clock now-in-millis)
   :request-handler
   (fn [_ _ _ context]
     (reset! context-spy context))})

(deftest
  lambda-handler-passes-normalised-context-to-request-handler-on-handle
  (import 'test.handler.ContextTest)
  (let [clock (clock/fixed-clock now-in-millis)
        aws-request-id (data/random-aws-request-id)
        function-name (data/random-function-name)
        function-version (data/random-function-version)
        remaining-time-in-millis (data/random-millisecond-duration)
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
         :remaining-time-in-millis remaining-time-in-millis
         :memory-limit-in-mb       memory-limit-in-mb
         :log-group-name           log-group-name
         :log-stream-name          log-stream-name
         :invoked-function-arn     invoked-function-arn
         :identity                 nil
         :client-context           nil}

        raw-context (data/raw-lambda-context context-options)
        normalised-context (context/normalise-context raw-context clock)

        lambda-handler (test.handler.ContextTest.)]
    (handlers/handle-request lambda-handler
      (request-input-stream {})
      (response-output-stream)
      raw-context)

    (is (= normalised-context (deref context-spy)))))

(def-lambda-handler
  {:name "test.handler.StateClockTest"
   :initialiser
   (fn []
     (let [now-in-millis (- (System/currentTimeMillis) 5000)]
       {:clock (clock/fixed-clock now-in-millis)}))
   :request-handler
   (fn [_ _ _ context]
     (reset! context-spy context))})

(deftest
  lambda-handler-uses-clock-on-state-when-present
  (import 'test.handler.StateClockTest)
  (let [aws-request-id (data/random-aws-request-id)
        function-name (data/random-function-name)
        function-version (data/random-function-version)
        remaining-time-in-millis (data/random-millisecond-duration)
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
         :remaining-time-in-millis remaining-time-in-millis
         :memory-limit-in-mb       memory-limit-in-mb
         :log-group-name           log-group-name
         :log-stream-name          log-stream-name
         :invoked-function-arn     invoked-function-arn
         :identity                 nil
         :client-context           nil}

        lambda-handler (test.handler.StateClockTest.)
        state (.state lambda-handler)
        clock (:clock state)

        raw-context (data/raw-lambda-context context-options)
        normalised-context (context/normalise-context raw-context clock)]
    (handlers/handle-request lambda-handler
      (request-input-stream {})
      (response-output-stream)
      raw-context)

    (is (= normalised-context (deref context-spy)))))

(def request-spy (atom nil))

(def-lambda-handler
  {:name "test.handler.RequestTest"
   :request-handler
   (fn [_ in _ _]
     (reset! request-spy (utils/<-json (io/reader in))))})

(deftest lambda-handler-passes-request-to-request-handler-on-handle
  (import 'test.handler.RequestTest)
  (let [request-event {"some" "event"}

        lambda-handler (test.handler.RequestTest.)]
    (handlers/handle-request lambda-handler
      (request-input-stream request-event)
      (response-output-stream)
      (data/raw-lambda-context))

    (is (= request-event (deref request-spy)))))

(def-lambda-handler
  {:name "test.handler.LoggingTest"
   :initialiser
   (fn [] {:clock (clock/fixed-clock (System/currentTimeMillis))
           :logger (ct/logger)})})

(deftest lambda-handler-logs-on-request-handling-started-on-handle
  (import 'test.handler.LoggingTest)
  (let [request-event {"some" "event"}

        lambda-handler (test.handler.LoggingTest.)
        state (.state lambda-handler)
        logger (:logger state)
        clock (:clock state)

        raw-context (data/raw-lambda-context)
        normalised-context (context/normalise-context raw-context clock)

        _ (handlers/handle-request lambda-handler
            (request-input-stream request-event)
            (response-output-stream)
            raw-context)

        log-event
        (first
          (filter
            #(= (:type %) ::request-handler.handling)
            (ct/events logger)))]
    (is (= :debug (:level log-event)))
    (is (= {:lambda {:state   state
                     :context normalised-context}}
          (:context log-event)))))
