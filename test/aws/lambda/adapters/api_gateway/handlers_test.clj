(ns aws.lambda.adapters.api-gateway.handlers-test
  (:require
   [clojure.java.io :as io]
   [clojure.test :refer [deftest is]]

   [cartus.test :as ct]

   [aws.lambda.adapters.utils :as utils]
   [aws.lambda.adapters.context :as context]
   [aws.lambda.adapters.handlers :as handlers]
   [aws.lambda.adapters.api-gateway.events :as events]
   [aws.lambda.adapters.api-gateway.transformers :as transformers]
   [aws.lambda.adapters.api-gateway.handlers
    :refer [def-api-gateway-handler
            def-api-gateway-ring-handler]]
   [aws.lambda.adapters.test-support.data :as data]
   [aws.lambda.adapters.test-support.clock :as clock])
  (:import
   [java.io ByteArrayOutputStream]))

(defn request-input-stream [event]
  (io/input-stream (.getBytes ^String (utils/->json event))))

(defn response-output-stream []
  (ByteArrayOutputStream.))

(def event-spy (atom nil))

(def-api-gateway-handler
  {:name "test.handler.api-gateway.EventTest"
   :request-handler
   (fn [_ event _]
     (reset! event-spy event))})

(deftest
  api-gateway-handler-passes-normalised-event-to-request-handler-on-handle
  (import 'test.handler.api-gateway.EventTest)
  (let [request-event (data/raw-api-gateway-v2-event)
        normalised-event (events/normalise-event request-event)

        lambda-handler (test.handler.api-gateway.EventTest.)]
    (handlers/handle-request lambda-handler
      (request-input-stream request-event)
      (response-output-stream)
      (data/raw-lambda-context))
    (is (= normalised-event (deref event-spy)))))

(def-api-gateway-handler
  {:name "test.handler.api-gateway.ResponseTest"
   :request-handler
   (fn [_ _ _]
     {:status-code     200
      :headers         {"content-type" "application/json"}
      :base64-encoded? false
      :body            "Hi!"})})

(deftest
  api-gateway-handler-writes-request-handler-response-to-output-stream-on-handle
  (import 'test.handler.api-gateway.ResponseTest)
  (let [lambda-handler (test.handler.api-gateway.ResponseTest.)
        response-output-stream (response-output-stream)]
    (handlers/handle-request lambda-handler
      (request-input-stream (data/raw-api-gateway-v2-event))
      response-output-stream
      (data/raw-lambda-context))
    (is (= {"statusCode"      200
            "headers"         {"content-type" "application/json"}
            "isBase64Encoded" false
            "body"            "Hi!"}
          (utils/<-json (str response-output-stream))))))

(def-api-gateway-handler
  {:name "test.handler.api-gateway.ExceptionTest"
   :request-handler
   (fn [_ _ _]
     (throw
       (ex-info "Everything is bad"
         {:some "context"})))})

(deftest
  api-gateway-handler-converts-exception-to-default-error-response-on-handle
  (import 'test.handler.api-gateway.ExceptionTest)
  (let [lambda-handler (test.handler.api-gateway.ExceptionTest.)
        response-output-stream (response-output-stream)]
    (handlers/handle-request lambda-handler
      (request-input-stream (data/raw-api-gateway-v2-event))
      response-output-stream
      (data/raw-lambda-context))
    (is (= {"statusCode"      500
            "headers"         {"content-type" "application/json"}
            "isBase64Encoded" false
            "body"            "{\"message\":\"Everything is bad\"}"}
          (utils/<-json (str response-output-stream))))))

(def-api-gateway-handler
  {:name "test.handler.api-gateway.ExceptionHandlerTest"
   :request-handler
   (fn [_ _ _]
     (throw
       (ex-info "Everything is bad"
         {:some "context"})))
   :exception-handler
   (fn [_]
     {:status-code     200
      :headers         {"content-type" "text/plain"}
      :base64-encoded? false
      :body            "Nothing to see here"})})

(deftest
  api-gateway-handler-uses-provided-exception-handler-on-exception-on-handle
  (import 'test.handler.api-gateway.ExceptionHandlerTest)
  (let [lambda-handler (test.handler.api-gateway.ExceptionHandlerTest.)
        response-output-stream (response-output-stream)]
    (handlers/handle-request lambda-handler
      (request-input-stream (data/raw-api-gateway-v2-event))
      response-output-stream
      (data/raw-lambda-context))
    (is (= {"statusCode"      200
            "headers"         {"content-type" "text/plain"}
            "isBase64Encoded" false
            "body"            "Nothing to see here"}
          (utils/<-json (str response-output-stream))))))

(def-api-gateway-handler
  {:name "test.handler.api-gateway.EventLoggingTest"
   :initialiser
   (fn [] {:clock  (clock/fixed-clock (System/currentTimeMillis))
           :logger (ct/logger)})})

(deftest api-gateway-handler-logs-on-event-read-on-handle
  (import 'test.handler.api-gateway.EventLoggingTest)
  (let [request-event (data/raw-api-gateway-v2-event)
        normalised-event (events/normalise-event request-event)

        lambda-handler (test.handler.api-gateway.EventLoggingTest.)
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
            #(= (:type %) ::request-handler.read-event)
            (ct/events logger)))]
    (is (= :debug (:level log-event)))
    (is (= {:lambda {:event   normalised-event
                     :context normalised-context}}
          (:context log-event)))))

(def-api-gateway-handler
  {:name "test.handler.api-gateway.ResponseLoggingTest"
   :initialiser
   (fn [] {:clock  (clock/fixed-clock (System/currentTimeMillis))
           :logger (ct/logger)})
   :request-handler
   (fn [_ _ _]
     {:status-code     200
      :headers         {"content-type" "application/json"}
      :base64-encoded? false
      :body            "Hi!"})})

(deftest api-gateway-handler-logs-on-response-written-on-handle
  (import 'test.handler.api-gateway.ResponseLoggingTest)
  (let [request-event (data/raw-api-gateway-v2-event)

        lambda-handler (test.handler.api-gateway.ResponseLoggingTest.)
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
            #(= (:type %) ::request-handler.writing-response)
            (ct/events logger)))]
    (is (= :debug (:level log-event)))
    (is (= {:lambda
            {:response
             {:status-code     200
              :headers         {"content-type" "application/json"}
              :base64-encoded? false
              :body            "Hi!"}
             :context normalised-context}}
          (:context log-event)))))

(def-api-gateway-handler
  {:name "test.handler.api-gateway.ExceptionLoggingTest"
   :initialiser
   (fn [] {:clock     (clock/fixed-clock (System/currentTimeMillis))
           :logger    (ct/logger)
           :exception (ex-info "It's borked"
                        {:some "context"})})
   :request-handler
   (fn [state _ _]
     (throw (:exception state)))})

(deftest api-gateway-handler-logs-on-exception-on-handle
  (import 'test.handler.api-gateway.ExceptionLoggingTest)
  (let [request-event (data/raw-api-gateway-v2-event)

        lambda-handler (test.handler.api-gateway.ExceptionLoggingTest.)
        state (.state lambda-handler)
        logger (:logger state)
        clock (:clock state)
        exception (:exception state)

        raw-context (data/raw-lambda-context)
        normalised-context (context/normalise-context raw-context clock)

        _ (handlers/handle-request lambda-handler
            (request-input-stream request-event)
            (response-output-stream)
            raw-context)

        log-event
        (first
          (filter
            #(= (:type %) ::request-handler.unhandled-exception)
            (ct/events logger)))]
    (is (= :error (:level log-event)))
    (is (= {:lambda {:context normalised-context}}
          (:context log-event)))
    (is (= exception
          (:exception log-event)))))

(def request-spy (atom nil))

(def-api-gateway-ring-handler
  {:name "test.handler.api-gateway.RingRequestTest"
   :initialiser
   (fn [] {:clock (clock/fixed-clock (System/currentTimeMillis))})
   :ring-handler
   (fn [request]
     (reset! request-spy request)
     {:status 200
      :body   "Hi!"})})

(deftest api-gateway-ring-handler-calls-ring-handler-with-ring-request-on-handle
  (import 'test.handler.api-gateway.RingRequestTest)
  (let [lambda-handler (test.handler.api-gateway.RingRequestTest.)
        state (.state lambda-handler)
        clock (:clock state)

        raw-context (data/raw-lambda-context)
        normalised-context (context/normalise-context raw-context clock)

        raw-event (data/raw-api-gateway-v2-event)
        normalised-event (events/normalise-event raw-event)

        ring-request
        (transformers/api-gateway-request->ring-request
          normalised-event normalised-context)]
    (handlers/handle-request lambda-handler
      (request-input-stream raw-event)
      (response-output-stream)
      raw-context)
    (let [expected-body (when (:body ring-request)
                          (slurp (:body ring-request)))
          actual-body (when (:body @request-spy)
                        (slurp (:body @request-spy)))]
      (is (= (dissoc ring-request :body)
            (dissoc @request-spy :body)))
      (is (= expected-body actual-body)))))

(def-api-gateway-ring-handler
  {:name "test.handler.api-gateway.RingResponseTest"
   :ring-handler
   (fn [_]
     {:status  201
      :headers {"Location" "https://example.com/123"}
      :body    "{\"id\": \"123\"}"})})

(deftest api-gateway-ring-handler-converts-ring-handler-response-on-handle
  (import 'test.handler.api-gateway.RingResponseTest)
  (let [lambda-handler (test.handler.api-gateway.RingResponseTest.)

        raw-context (data/raw-lambda-context)
        raw-event (data/raw-api-gateway-v2-event)

        response-output-stream (response-output-stream)]
    (handlers/handle-request lambda-handler
      (request-input-stream raw-event)
      response-output-stream
      raw-context)
    (is (= {"statusCode"        201
            "headers"           {"location" "https://example.com/123"}
            "multiValueHeaders" {}
            "isBase64Encoded"   false
            "body"              "{\"id\": \"123\"}"}
          (utils/<-json (str response-output-stream))))))
