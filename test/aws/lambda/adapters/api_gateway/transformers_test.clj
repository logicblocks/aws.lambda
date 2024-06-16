(ns aws.lambda.adapters.api-gateway.transformers-test
  (:require
   [clojure.java.io :as io]
   [clojure.test :refer [deftest is]]

   [ring.util.codec :as ring-codec]
   [ring.util.response :as ring-response]

   [aws.lambda.adapters.api-gateway.transformers :as transformers]
   [aws.lambda.adapters.api-gateway.protocols :as protocols]
   [aws.lambda.adapters.test-support.data :as data])
  (:import
   [java.io File]))

;; ->ring-request-body
(deftest ->ring-request-body-converts-plain-string-to-input-stream
  (let [body "{\"hello\": \"world\"}"
        base64? false
        input-stream (protocols/->ring-request-body body base64?)]
    (is (= body (slurp input-stream)))))

(deftest ->ring-request-body-converts-base64-encoded-string-to-input-stream
  (let [body "{\"hello\": \"world\"}"
        base64-body "eyJoZWxsbyI6ICJ3b3JsZCJ9"
        base64? true
        result (protocols/->ring-request-body base64-body base64?)]
    (is (= body (slurp result)))))

(deftest ->ring-request-body-puns-nil
  (let [body nil
        base64? false
        result (protocols/->ring-request-body body base64?)]
    (is (nil? result))))

(deftest ->ring-request-body-prints-objects-to-input-stream
  (let [body 500
        base64? false
        result (protocols/->ring-request-body body base64?)]
    (is (= "500" (slurp result)))))

;; ->api-gateway-response-body
(deftest ->api-gateway-response-body-passes-through-string-body-untouched
  (let [body "{\"hello\": \"world\"}"

        result (protocols/->api-gateway-response-body body)]
    (is (= body (:body result)))))

(deftest ->api-gateway-response-body-marks-string-body-as-unencoded
  (let [body "{\"hello\": \"world\"}"

        result (protocols/->api-gateway-response-body body)]
    (is (false? (:base64-encoded? result)))))

(deftest ->api-gateway-response-body-reads-and-encodes-file-body
  (let [file (doto (File/createTempFile "test-" ".tmp")
               (.deleteOnExit))

        _ (with-open [f (io/writer file)]
            (.write f "Hello world!"))

        body file

        result (protocols/->api-gateway-response-body body)]
    (is (= (ring-codec/base64-encode (.getBytes "Hello world!"))
          (:body result)))))

(deftest ->api-gateway-response-body-marks-file-body-as-encoded
  (let [file (doto (File/createTempFile "test-" ".tmp")
               (.deleteOnExit))

        _ (with-open [f (io/writer file)]
            (.write f "Hello world!"))

        body file

        result (protocols/->api-gateway-response-body body)]
    (is (true? (:base64-encoded? result)))))

(deftest ->api-gateway-response-body-passes-through-nil-body-untouched
  (let [body nil

        result (protocols/->api-gateway-response-body body)]
    (is (= body (:body result)))))

(deftest ->api-gateway-response-body-marks-nil-body-as-unencoded
  (let [body nil

        result (protocols/->api-gateway-response-body body)]
    (is (false? (:base64-encoded? result)))))

(deftest ->api-gateway-response-body-base64-encodes-input-stream-body
  (let [body (io/input-stream (.getBytes "Banana"))

        result (protocols/->api-gateway-response-body body)]
    (is (= (ring-codec/base64-encode (.getBytes "Banana"))
          (:body result)))))

(deftest ->api-gateway-response-body-marks-input-stream-body-as-encoded
  (let [body (io/input-stream (.getBytes "Banana"))

        result (protocols/->api-gateway-response-body body)]
    (is (true? (:base64-encoded? result)))))

(deftest ->api-gateway-response-body-base64-strings-and-joins-seq-body
  (let [body (seq [1 2 3 4 5])

        result (protocols/->api-gateway-response-body body)]
    (is (= "12345" (:body result)))))

(deftest ->api-gateway-response-body-marks-seq-body-as-unencoded
  (let [body (seq [1 2 3 4 5])

        result (protocols/->api-gateway-response-body body)]
    (is (false? (:base64-encoded? result)))))

;; ring request for v1 api gateway events
(deftest ring-request-gets-server-name-from-via-header-v1
  (let [via-host "service.example.com"

        event (data/normalised-api-gateway-v1-event
                {:headers {"via" (data/via-header-value
                                   {:host via-host})}})
        context (data/normalised-lambda-context)

        request (transformers/api-gateway-request->ring-request event context)]
    (is (= via-host (:server-name request)))))

(deftest ring-request-server-name-strips-port-from-via-header-v1
  (let [via-host "service.example.com"
        via-address (str via-host ":8080")

        event (data/normalised-api-gateway-v1-event
                {:headers {"via" (data/via-header-value
                                   {:host via-address})}})
        context (data/normalised-lambda-context)

        request (transformers/api-gateway-request->ring-request event context)]
    (is (= via-host (:server-name request)))))

(deftest ring-request-gets-server-name-from-host-header-when-no-via-header-v1
  (let [host "service.example.com"

        event (data/normalised-api-gateway-v1-event
                {:headers {"host" host}})
        context (data/normalised-lambda-context)

        request (transformers/api-gateway-request->ring-request event context)]
    (is (= host (:server-name request)))))

(deftest ring-request-server-name-strips-port-from-host-header-v1
  (let [host "service.example.com"
        address (str host ":8080")

        event (data/normalised-api-gateway-v1-event
                {:headers {"host" address}})
        context (data/normalised-lambda-context)

        request (transformers/api-gateway-request->ring-request event context)]
    (is (= host (:server-name request)))))

(deftest ring-request-has-nil-server-name-when-no-headers-v1
  (let [event (data/normalised-api-gateway-v1-event
                {:headers {"host" nil}})
        context (data/normalised-lambda-context)

        request (transformers/api-gateway-request->ring-request event context)]
    (is (nil? (:server-name request)))))

(deftest ring-request-gets-server-port-from-x-forwarded-port-header-v1
  (let [x-forwarded-port 8080

        event (data/normalised-api-gateway-v1-event
                {:headers {"x-forwarded-port" (str x-forwarded-port)}})
        context (data/normalised-lambda-context)

        request (transformers/api-gateway-request->ring-request event context)]
    (is (= x-forwarded-port (:server-port request)))))

(deftest ring-request-has-nil-server-port-when-no-x-forwarded-port-header-v1
  (let [event (data/normalised-api-gateway-v1-event)
        context (data/normalised-lambda-context)

        request (transformers/api-gateway-request->ring-request event context)]
    (is (nil? (:server-port request)))))

(deftest ring-request-has-default-server-port-when-no-header-and-provided-v1
  (let [default-server-port 443
        event (data/normalised-api-gateway-v1-event)
        context (data/normalised-lambda-context)
        options {:defaults {:server-port default-server-port}}

        request (transformers/api-gateway-request->ring-request
                  event context options)]
    (is (= default-server-port (:server-port request)))))

(deftest ring-request-gets-remote-address-from-identity-source-ip-v1
  (let [request-context-identity-source-ip "1.2.3.4"

        event (data/normalised-api-gateway-v1-event
                {:request-context
                 (data/normalised-api-gateway-v1-request-context
                   {:identity
                    (data/normalised-api-gateway-v1-request-identity
                      {:source-ip request-context-identity-source-ip})})})
        context (data/normalised-lambda-context)

        request (transformers/api-gateway-request->ring-request event context)]
    (is (= request-context-identity-source-ip (:remote-addr request)))))

(deftest ring-request-has-nil-remote-address-when-no-identity-source-ip-v1
  (let [event (data/normalised-api-gateway-v1-event
                {:request-context
                 (data/normalised-api-gateway-v1-request-context
                   {:identity
                    (data/normalised-api-gateway-v1-request-identity
                      {:source-ip nil})})})
        context (data/normalised-lambda-context)

        request (transformers/api-gateway-request->ring-request event context)]
    (is (nil? (:remote-addr request)))))

(deftest ring-request-gets-uri-from-path-v1
  (let [path "/path/to/file"

        event (data/normalised-api-gateway-v1-event
                {:path path})
        context (data/normalised-lambda-context)

        request (transformers/api-gateway-request->ring-request event context)]
    (is (= path (:uri request)))))

(deftest ring-request-gets-uri-from-request-context-path-when-raw-v1
  (let [path "/path/to/file"
        raw-path "/stage/path/to/file"

        event (data/normalised-api-gateway-v1-event
                {:path path
                 :request-context
                 (data/normalised-api-gateway-v1-request-context
                   {:path raw-path})})
        context (data/normalised-lambda-context)

        request (transformers/api-gateway-request->ring-request event context
                  {:options {:use-raw-path? true}})]
    (is (= raw-path (:uri request)))))

(deftest ring-request-gets-query-string-from-query-string-parameters-v1
  (let [query-string-parameters {"foo" "bar"
                                 "baz" "qux"}
        query-string "foo=bar&baz=qux"

        event (data/normalised-api-gateway-v1-event
                {:query-string-parameters query-string-parameters})
        context (data/normalised-lambda-context)

        request (transformers/api-gateway-request->ring-request event context)]
    (is (= query-string (:query-string request)))))

(deftest ring-request-gets-nil-query-string-when-query-string-parameters-nil-v1
  (let [query-string-parameters nil
        query-string nil

        event (data/normalised-api-gateway-v1-event
                {:query-string-parameters query-string-parameters})
        context (data/normalised-lambda-context)

        request (transformers/api-gateway-request->ring-request event context)]
    (is (= query-string (:query-string request)))))

(deftest ring-request-gets-scheme-from-x-forwarded-proto-header-v1
  (let [x-forwarded-proto "https"
        scheme :https

        event (data/normalised-api-gateway-v1-event
                {:headers {"x-forwarded-proto" x-forwarded-proto}})
        context (data/normalised-lambda-context)

        request (transformers/api-gateway-request->ring-request event context)]
    (is (= scheme (:scheme request)))))

(deftest ring-request-has-nil-scheme-when-no-x-forwarded-proto-header-v1
  (let [event (data/normalised-api-gateway-v1-event)
        context (data/normalised-lambda-context)

        request (transformers/api-gateway-request->ring-request event context)]
    (is (nil? (:scheme request)))))

(deftest ring-request-has-default-scheme-when-no-header-and-provided-v1
  (let [default-scheme "https"
        event (data/normalised-api-gateway-v1-event)
        context (data/normalised-lambda-context)
        options {:defaults {:scheme default-scheme}}

        request (transformers/api-gateway-request->ring-request
                  event context options)]
    (is (= default-scheme (:scheme request)))))

(deftest ring-request-gets-request-method-from-http-method-v1
  (let [http-method "PUT"
        request-method :put

        event (data/normalised-api-gateway-v1-event
                {:http-method http-method})
        context (data/normalised-lambda-context)

        request (transformers/api-gateway-request->ring-request event context)]
    (is (= request-method (:request-method request)))))

(deftest ring-request-has-nil-request-method-when-no-http-method-v1
  (let [event (data/normalised-api-gateway-v1-event
                {:http-method nil})
        context (data/normalised-lambda-context)

        request (transformers/api-gateway-request->ring-request event context)]
    (is (nil? (:request-method request)))))

(deftest ring-request-gets-protocol-from-request-context-v1
  (let [request-context-protocol "HTTP/1.1"

        event (data/normalised-api-gateway-v1-event
                {:request-context
                 (data/normalised-api-gateway-v1-request-context
                   {:protocol request-context-protocol})})
        context (data/normalised-lambda-context)

        request (transformers/api-gateway-request->ring-request event context)]
    (is (= request-context-protocol (:protocol request)))))

(deftest ring-request-includes-headers-v1
  (let [headers
        {"host"   "a3ed23f1.execute-api.eu-west-2.amazonaws.com"
         "accept" "text/html,application/xhtml+xml,application/xml;q=0.9"}

        event (data/normalised-api-gateway-v1-event
                {:headers headers})
        context (data/normalised-lambda-context)

        request (transformers/api-gateway-request->ring-request event context)]
    (is (= headers (:headers request)))))

(deftest ring-request-gets-body-input-stream-from-plain-body-v1
  (let [body "{\"hello\": \"world\"}"
        base64-encoded? false

        event (data/normalised-api-gateway-v1-event
                {:body            body
                 :base64-encoded? base64-encoded?})
        context (data/normalised-lambda-context)

        request (transformers/api-gateway-request->ring-request event context)]
    (is (= body (slurp (:body request))))))

(deftest ring-request-gets-body-input-stream-from-base64-encoded-body-v1
  (let [body "{\"hello\": \"world\"}"
        base64-body "eyJoZWxsbyI6ICJ3b3JsZCJ9"
        base64-encoded? true

        event (data/normalised-api-gateway-v1-event
                {:body            base64-body
                 :base64-encoded? base64-encoded?})
        context (data/normalised-lambda-context)

        request (transformers/api-gateway-request->ring-request event context)]
    (is (= body (slurp (:body request))))))

(deftest ring-request-has-nil-body-when-event-body-nil-v1
  (let [body nil

        event (data/normalised-api-gateway-v1-event
                {:body body})
        context (data/normalised-lambda-context)

        request (transformers/api-gateway-request->ring-request event context)]
    (is (nil? (:body request)))))

(deftest ring-request-includes-full-event-on-request-v1
  (let [event (data/normalised-api-gateway-v1-event)
        context (data/normalised-lambda-context)

        request (transformers/api-gateway-request->ring-request event context)]
    (is (= event (get-in request [:lambda :event])))))

(deftest ring-request-includes-full-context-on-request-v1
  (let [event (data/normalised-api-gateway-v1-event)
        context (data/normalised-lambda-context)

        request (transformers/api-gateway-request->ring-request event context)]
    (is (= context (get-in request [:lambda :context])))))

;; ring request for v2 api gateway events
(deftest ring-request-gets-server-name-from-via-header-v2
  (let [via-host "service.example.com"

        event (data/normalised-api-gateway-v2-event
                {:headers {"via" (data/via-header-value
                                   {:host via-host})}})
        context (data/normalised-lambda-context)

        request (transformers/api-gateway-request->ring-request event context)]
    (is (= via-host (:server-name request)))))

(deftest ring-request-server-name-strips-port-from-via-header-v2
  (let [via-host "service.example.com"
        via-address (str via-host ":8080")

        event (data/normalised-api-gateway-v2-event
                {:headers {"via" (data/via-header-value
                                   {:host via-address})}})
        context (data/normalised-lambda-context)

        request (transformers/api-gateway-request->ring-request event context)]
    (is (= via-host (:server-name request)))))

(deftest ring-request-gets-server-name-from-host-header-when-no-via-header-v2
  (let [host "service.example.com"

        event (data/normalised-api-gateway-v2-event
                {:headers {"host" host}})
        context (data/normalised-lambda-context)

        request (transformers/api-gateway-request->ring-request event context)]
    (is (= host (:server-name request)))))

(deftest ring-request-server-name-strips-port-from-host-header-v2
  (let [host "service.example.com"
        address (str host ":8080")

        event (data/normalised-api-gateway-v2-event
                {:headers {"host" address}})
        context (data/normalised-lambda-context)

        request (transformers/api-gateway-request->ring-request event context)]
    (is (= host (:server-name request)))))

(deftest ring-request-has-nil-server-name-when-no-headers-v2
  (let [event (data/normalised-api-gateway-v2-event
                {:headers {"host" nil}})
        context (data/normalised-lambda-context)

        request (transformers/api-gateway-request->ring-request event context)]
    (is (nil? (:server-name request)))))

(deftest ring-request-gets-server-port-from-x-forwarded-port-header-v2
  (let [x-forwarded-port 8080

        event (data/normalised-api-gateway-v2-event
                {:headers {"x-forwarded-port" (str x-forwarded-port)}})
        context (data/normalised-lambda-context)

        request (transformers/api-gateway-request->ring-request event context)]
    (is (= x-forwarded-port (:server-port request)))))

(deftest ring-request-has-nil-server-port-when-no-x-forwarded-port-header-v2
  (let [event (data/normalised-api-gateway-v2-event)
        context (data/normalised-lambda-context)

        request (transformers/api-gateway-request->ring-request event context)]
    (is (nil? (:server-port request)))))

(deftest ring-request-has-default-server-port-when-no-header-and-provided-v2
  (let [default-server-port 443
        event (data/normalised-api-gateway-v2-event)
        context (data/normalised-lambda-context)
        options {:defaults {:server-port default-server-port}}

        request (transformers/api-gateway-request->ring-request
                  event context options)]
    (is (= default-server-port (:server-port request)))))

(deftest ring-request-gets-remote-address-from-http-source-ip-v2
  (let [request-context-http-source-ip "1.2.3.4"

        event (data/normalised-api-gateway-v2-event
                {:request-context
                 (data/normalised-api-gateway-v2-request-context
                   {:http
                    (data/normalised-api-gateway-v2-http-context
                      {:source-ip request-context-http-source-ip})})})
        context (data/normalised-lambda-context)

        request (transformers/api-gateway-request->ring-request event context)]
    (is (= request-context-http-source-ip (:remote-addr request)))))

(deftest ring-request-has-nil-remote-address-when-no-http-source-ip-v2
  (let [event (data/normalised-api-gateway-v2-event
                {:request-context
                 (data/normalised-api-gateway-v2-request-context
                   {:http
                    (data/normalised-api-gateway-v2-http-context
                      {:source-ip nil})})})
        context (data/normalised-lambda-context)

        request (transformers/api-gateway-request->ring-request event context)]
    (is (nil? (:remote-addr request)))))

(deftest ring-request-gets-uri-from-http-path-v2
  (let [path "/path/to/file"
        raw-path "/stage/path/to/file"

        event (data/normalised-api-gateway-v2-event
                {:request-context
                 (data/normalised-api-gateway-v2-request-context
                   {:http
                    (data/normalised-api-gateway-v2-http-context
                      {:path path})})})
        context (data/normalised-lambda-context)

        request (transformers/api-gateway-request->ring-request event context)]
    (is (= path (:uri request)))))

(deftest ring-request-gets-uri-from-raw-path-when-raw-v2
  (let [path "/path/to/file"
        raw-path "/stage/path/to/file"

        event (data/normalised-api-gateway-v2-event
                {:raw-path raw-path
                 :request-context
                 (data/normalised-api-gateway-v2-request-context
                   {:http
                    (data/normalised-api-gateway-v2-http-context
                      {:path path})})})
        context (data/normalised-lambda-context)

        request (transformers/api-gateway-request->ring-request event context
                  {:options {:use-raw-path? true}})]
    (is (= raw-path (:uri request)))))

(deftest ring-request-gets-query-string-from-raw-query-string-v2
  (let [raw-query-string "foo=bar&baz=qux"

        event (data/normalised-api-gateway-v2-event
                {:query-string-parameters nil
                 :raw-query-string        raw-query-string})
        context (data/normalised-lambda-context)

        request (transformers/api-gateway-request->ring-request event context)]
    (is (= raw-query-string (:query-string request)))))

(deftest ring-request-gets-nil-query-string-when-raw-query-string-nil-v2
  (let [raw-query-string nil

        event (data/normalised-api-gateway-v2-event
                {:query-string-parameters nil
                 :raw-query-string        raw-query-string})
        context (data/normalised-lambda-context)

        request (transformers/api-gateway-request->ring-request event context)]
    (is (= raw-query-string (:query-string request)))))

(deftest ring-request-gets-scheme-from-x-forwarded-proto-header-v2
  (let [x-forwarded-proto "https"
        scheme :https

        event (data/normalised-api-gateway-v2-event
                {:headers {"x-forwarded-proto" x-forwarded-proto}})
        context (data/normalised-lambda-context)

        request (transformers/api-gateway-request->ring-request event context)]
    (is (= scheme (:scheme request)))))

(deftest ring-request-has-nil-scheme-when-no-x-forwarded-proto-header-v2
  (let [event (data/normalised-api-gateway-v2-event)
        context (data/normalised-lambda-context)

        request (transformers/api-gateway-request->ring-request event context)]
    (is (nil? (:scheme request)))))

(deftest ring-request-has-default-scheme-when-no-header-and-provided-v2
  (let [default-scheme "https"
        event (data/normalised-api-gateway-v2-event)
        context (data/normalised-lambda-context)
        options {:defaults {:scheme default-scheme}}

        request (transformers/api-gateway-request->ring-request
                  event context options)]
    (is (= default-scheme (:scheme request)))))

(deftest ring-request-gets-request-method-from-http-method-v2
  (let [http-method "PUT"
        request-method :put

        event (data/normalised-api-gateway-v2-event
                {:request-context
                 (data/normalised-api-gateway-v2-request-context
                   {:http
                    (data/normalised-api-gateway-v2-http-context
                      {:method http-method})})})
        context (data/normalised-lambda-context)

        request (transformers/api-gateway-request->ring-request event context)]
    (is (= request-method (:request-method request)))))

(deftest ring-request-has-nil-request-method-when-no-http-method-v2
  (let [event (data/normalised-api-gateway-v2-event
                {:request-context
                 (data/normalised-api-gateway-v2-request-context
                   {:http
                    (data/normalised-api-gateway-v2-http-context
                      {:method nil})})})
        context (data/normalised-lambda-context)

        request (transformers/api-gateway-request->ring-request event context)]
    (is (nil? (:request-method request)))))

(deftest ring-request-gets-protocol-from-http-protocol-v2
  (let [protocol "HTTP/1.1"

        event (data/normalised-api-gateway-v2-event
                {:request-context
                 (data/normalised-api-gateway-v2-request-context
                   {:http
                    (data/normalised-api-gateway-v2-http-context
                      {:protocol protocol})})})
        context (data/normalised-lambda-context)

        request (transformers/api-gateway-request->ring-request event context)]
    (is (= protocol (:protocol request)))))

(deftest ring-request-includes-headers-v2
  (let [headers
        {"host"   "a3ed23f1.execute-api.eu-west-2.amazonaws.com"
         "accept" "text/html,application/xhtml+xml,application/xml;q=0.9"}

        event (data/normalised-api-gateway-v2-event
                {:headers headers})
        context (data/normalised-lambda-context)

        request (transformers/api-gateway-request->ring-request event context)]
    (is (= headers (:headers request)))))

(deftest ring-request-gets-body-input-stream-from-plain-body-v2
  (let [body "{\"hello\": \"world\"}"
        base64-encoded? false

        event (data/normalised-api-gateway-v2-event
                {:body            body
                 :base64-encoded? base64-encoded?})
        context (data/normalised-lambda-context)

        request (transformers/api-gateway-request->ring-request event context)]
    (is (= body (slurp (:body request))))))

(deftest ring-request-gets-body-input-stream-from-base64-encoded-body-v2
  (let [body "{\"hello\": \"world\"}"
        base64-body "eyJoZWxsbyI6ICJ3b3JsZCJ9"
        base64-encoded? true

        event (data/normalised-api-gateway-v2-event
                {:body            base64-body
                 :base64-encoded? base64-encoded?})
        context (data/normalised-lambda-context)

        request (transformers/api-gateway-request->ring-request event context)]
    (is (= body (slurp (:body request))))))

(deftest ring-request-has-nil-body-when-event-body-nil-v2
  (let [body nil

        event (data/normalised-api-gateway-v2-event
                {:body body})
        context (data/normalised-lambda-context)

        request (transformers/api-gateway-request->ring-request event context)]
    (is (nil? (:body request)))))

(deftest ring-request-includes-full-event-on-request-v2
  (let [event (data/normalised-api-gateway-v2-event)
        context (data/normalised-lambda-context)

        request (transformers/api-gateway-request->ring-request event context)]
    (is (= event (get-in request [:lambda :event])))))

(deftest ring-request-includes-full-context-on-request-v2
  (let [event (data/normalised-api-gateway-v2-event)
        context (data/normalised-lambda-context)

        request (transformers/api-gateway-request->ring-request event context)]
    (is (= context (get-in request [:lambda :context])))))

;; api gateway response for v1 api gateway events
(deftest api-gateway-response-gets-status-code-from-ring-response-status-v1
  (let [status 201
        ring-response (ring-response/status status)

        api-gateway-response
        (transformers/ring-response->api-gateway-response ring-response
          {:version "1.0"})]
    (is (= status (:status-code api-gateway-response)))))

(deftest api-gateway-response-uses-string-body-from-ring-response-v1
  (let [body "{\"hello\": \"world\"}"
        ring-response (ring-response/response body)

        api-gateway-response
        (transformers/ring-response->api-gateway-response ring-response
          {:version "1.0"})]
    (is (= body (:body api-gateway-response)))))

(deftest
  api-gateway-response-has-base64-encoded?-false-for-string-ring-response-v1
  (let [body "{\"hello\": \"world\"}"
        ring-response (ring-response/response body)

        api-gateway-response
        (transformers/ring-response->api-gateway-response ring-response
          {:version "1.0"})]
    (is (false? (:base64-encoded? api-gateway-response)))))

(deftest api-gateway-response-uses-file-body-from-ring-response-v1
  (let [file (doto (File/createTempFile "test-" ".tmp")
               (.deleteOnExit))

        _ (with-open [f (io/writer file)]
            (.write f "Goodbye cruel world!"))

        body file
        ring-response (ring-response/response body)

        api-gateway-response
        (transformers/ring-response->api-gateway-response ring-response
          {:version "1.0"})]
    (is (= (ring-codec/base64-encode (.getBytes "Goodbye cruel world!"))
          (:body api-gateway-response)))))

(deftest
  api-gateway-response-has-base64-encoded?-true-for-file-ring-response-v1
  (let [file (doto (File/createTempFile "test-" ".tmp")
               (.deleteOnExit))

        _ (with-open [f (io/writer file)]
            (.write f "Goodbye cruel world!"))

        body file
        ring-response (ring-response/response body)

        api-gateway-response
        (transformers/ring-response->api-gateway-response ring-response
          {:version "1.0"})]
    (is (true? (:base64-encoded? api-gateway-response)))))

(deftest api-gateway-response-passes-through-nil-body-from-ring-response-v1
  (let [body nil
        ring-response (ring-response/response body)

        result (transformers/ring-response->api-gateway-response ring-response
                 {:version "1.0"})]
    (is (= body (:body result)))))

(deftest
  api-gateway-response-has-base64-encoded?-false-for-nil-ring-response-body-v1
  (let [body nil
        ring-response (ring-response/response body)

        result (transformers/ring-response->api-gateway-response ring-response
                 {:version "1.0"})]
    (is (false? (:base64-encoded? result)))))

(deftest api-gateway-response-encodes-input-stream-ring-response-body-v1
  (let [body (io/input-stream (.getBytes "Banana"))
        ring-response (ring-response/response body)

        result (transformers/ring-response->api-gateway-response ring-response
                 {:version "1.0"})]
    (is (= (ring-codec/base64-encode (.getBytes "Banana"))
          (:body result)))))

(deftest
  api-gateway-response-has-base64-encoded?-true-for-input-stream-body-v1
  (let [body (io/input-stream (.getBytes "Banana"))
        ring-response (ring-response/response body)

        result (transformers/ring-response->api-gateway-response ring-response
                 {:version "1.0"})]
    (is (true? (:base64-encoded? result)))))

(deftest api-gateway-response-strings-and-joins-seq-ring-response-body-v1
  (let [body (seq [1 2 3 4 5])
        ring-response (ring-response/response body)

        result (transformers/ring-response->api-gateway-response ring-response
                 {:version "1.0"})]
    (is (= "12345" (:body result)))))

(deftest
  api-gateway-response-has-base64-encoded?-false-for-seq-ring-response-body-v1
  (let [body (seq [1 2 3 4 5])
        ring-response (ring-response/response body)

        result (transformers/ring-response->api-gateway-response ring-response
                 {:version "1.0"})]
    (is (false? (:base64-encoded? result)))))

(deftest
  api-gateway-response-gets-headers-from-non-cookie-ring-response-headers-v1
  (let [headers {"Content-Type"     "application/hal+json"
                 "Content-Encoding" "gzip"
                 "Cache-Control"    ["no-cache" "must-revalidate"]
                 "Set-Cookie"
                 ["key1=value1; domain=.example.com"
                  "key2=value2; domain=.other.example.com"]}
        ring-response (assoc (ring-response/response "OK") :headers headers)

        result (transformers/ring-response->api-gateway-response ring-response
                 {:version "1.0"})]
    (is (= {"content-type"     "application/hal+json"
            "content-encoding" "gzip"
            "cache-control"    "no-cache, must-revalidate"}
          (:headers result)))))

(deftest
  api-gateway-response-gets-multi-value-headers-from-set-cookie-header-v1
  (let [headers {"Content-Type"     "application/hal+json"
                 "Content-Encoding" "gzip"
                 "Cache-Control"    ["no-cache" "must-revalidate"]
                 "Set-Cookie"
                 ["key1=value1; domain=.example.com"
                  "key2=value2; domain=.other.example.com"]}
        ring-response (assoc (ring-response/response "OK") :headers headers)

        result (transformers/ring-response->api-gateway-response ring-response
                 {:version "1.0"})]
    (is (= {"set-cookie" ["key1=value1; domain=.example.com"
                          "key2=value2; domain=.other.example.com"]}
          (:multi-value-headers result)))))

;; api gateway response for v2 api gateway events
(deftest api-gateway-response-gets-status-code-from-ring-response-status-v2
  (let [status 201
        ring-response (ring-response/status status)

        api-gateway-response
        (transformers/ring-response->api-gateway-response ring-response
          {:version "2.0"})]
    (is (= status (:status-code api-gateway-response)))))

(deftest api-gateway-response-uses-string-body-from-ring-response-v2
  (let [body "{\"hello\": \"world\"}"
        ring-response (ring-response/response body)

        api-gateway-response
        (transformers/ring-response->api-gateway-response ring-response
          {:version "2.0"})]
    (is (= body (:body api-gateway-response)))))

(deftest
  api-gateway-response-has-base64-encoded?-false-for-string-ring-response-v2
  (let [body "{\"hello\": \"world\"}"
        ring-response (ring-response/response body)

        api-gateway-response
        (transformers/ring-response->api-gateway-response ring-response
          {:version "2.0"})]
    (is (false? (:base64-encoded? api-gateway-response)))))

(deftest api-gateway-response-uses-file-body-from-ring-response-v2
  (let [file (doto (File/createTempFile "test-" ".tmp")
               (.deleteOnExit))

        _ (with-open [f (io/writer file)]
            (.write f "Goodbye cruel world!"))

        body file
        ring-response (ring-response/response body)

        api-gateway-response
        (transformers/ring-response->api-gateway-response ring-response
          {:version "2.0"})]
    (is (= (ring-codec/base64-encode (.getBytes "Goodbye cruel world!"))
          (:body api-gateway-response)))))

(deftest
  api-gateway-response-has-base64-encoded?-true-for-file-ring-response-v2
  (let [file (doto (File/createTempFile "test-" ".tmp")
               (.deleteOnExit))

        _ (with-open [f (io/writer file)]
            (.write f "Goodbye cruel world!"))

        body file
        ring-response (ring-response/response body)

        api-gateway-response
        (transformers/ring-response->api-gateway-response ring-response
          {:version "2.0"})]
    (is (true? (:base64-encoded? api-gateway-response)))))

(deftest api-gateway-response-passes-through-nil-body-from-ring-response-v2
  (let [body nil
        ring-response (ring-response/response body)

        result (transformers/ring-response->api-gateway-response ring-response
                 {:version "2.0"})]
    (is (= body (:body result)))))

(deftest
  api-gateway-response-has-base64-encoded?-false-for-nil-ring-response-body-v2
  (let [body nil
        ring-response (ring-response/response body)

        result (transformers/ring-response->api-gateway-response ring-response
                 {:version "2.0"})]
    (is (false? (:base64-encoded? result)))))

(deftest api-gateway-response-encodes-input-stream-ring-response-body-v2
  (let [body (io/input-stream (.getBytes "Banana"))
        ring-response (ring-response/response body)

        result (transformers/ring-response->api-gateway-response ring-response
                 {:version "2.0"})]
    (is (= (ring-codec/base64-encode (.getBytes "Banana"))
          (:body result)))))

(deftest
  api-gateway-response-has-base64-encoded?-true-for-input-stream-body-v2
  (let [body (io/input-stream (.getBytes "Banana"))
        ring-response (ring-response/response body)

        result (transformers/ring-response->api-gateway-response ring-response
                 {:version "2.0"})]
    (is (true? (:base64-encoded? result)))))

(deftest api-gateway-response-strings-and-joins-seq-ring-response-body-v2
  (let [body (seq [1 2 3 4 5])
        ring-response (ring-response/response body)

        result (transformers/ring-response->api-gateway-response ring-response
                 {:version "2.0"})]
    (is (= "12345" (:body result)))))

(deftest
  api-gateway-response-has-base64-encoded?-false-for-seq-ring-response-body-v2
  (let [body (seq [1 2 3 4 5])
        ring-response (ring-response/response body)

        result (transformers/ring-response->api-gateway-response ring-response
                 {:version "2.0"})]
    (is (false? (:base64-encoded? result)))))

(deftest
  api-gateway-response-gets-headers-from-non-cookie-ring-response-headers-v2
  (let [headers {"Content-Type"     "application/hal+json"
                 "Content-Encoding" "gzip"
                 "Cache-Control"    ["no-cache" "must-revalidate"]
                 "Set-Cookie"
                 ["key1=value1; domain=.example.com"
                  "key2=value2; domain=.other.example.com"]}
        ring-response (assoc (ring-response/response "OK") :headers headers)

        result (transformers/ring-response->api-gateway-response ring-response
                 {:version "2.0"})]
    (is (= {"content-type"     "application/hal+json"
            "content-encoding" "gzip"
            "cache-control"    "no-cache, must-revalidate"}
          (:headers result)))))

(deftest
  api-gateway-response-gets-cookies-from-set-cookie-header-value-v2
  (let [headers {"Content-Type"     "application/hal+json"
                 "Content-Encoding" "gzip"
                 "Cache-Control"    ["no-cache" "must-revalidate"]
                 "Set-Cookie"
                 ["key1=value1; domain=.example.com"
                  "key2=value2; domain=.other.example.com"]}
        ring-response (assoc (ring-response/response "OK") :headers headers)

        result (transformers/ring-response->api-gateway-response ring-response
                 {:version "2.0"})]
    (is (= ["key1=value1; domain=.example.com"
            "key2=value2; domain=.other.example.com"]
          (:cookies result)))))
