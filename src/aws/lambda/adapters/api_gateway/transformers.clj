(ns aws.lambda.adapters.api-gateway.transformers
  (:require
    [clojure.java.io :as io]
    [clojure.string :as string]

    [camel-snake-kebab.core :as csk-core]

    [ring.util.io :as ring-io]
    [ring.util.codec :as ring-codec]

    [aws.lambda.adapters.utils :as utils]
    [aws.lambda.adapters.api-gateway.events :as events]
    [aws.lambda.adapters.api-gateway.protocols :as protocols])
  (:import
    [clojure.lang ISeq]
    [java.io File InputStream]
    [java.nio.file Files]))

(extend-protocol protocols/RequestBodyTransformer
  String
  (->ring-request-body [^String body base64-encoded?]
    (if base64-encoded?
      (io/input-stream (ring-codec/base64-decode body))
      (ring-io/string-input-stream body)))

  Object
  (->ring-request-body [^Object body _]
    (ring-io/string-input-stream
      (pr-str body)))

  nil
  (->ring-request-body [_ _]
    nil))

(extend-protocol protocols/ResponseBodyTransformer
  InputStream
  (->api-gateway-response-body [^InputStream body]
    {:body            (ring-codec/base64-encode (.readAllBytes body))
     :base64-encoded? true})

  String
  (->api-gateway-response-body [^String body]
    {:body            body
     :base64-encoded? false})

  File
  (->api-gateway-response-body [^File body]
    {:body            (ring-codec/base64-encode
                        (Files/readAllBytes (.toPath body)))
     :base64-encoded? true})

  ISeq
  (->api-gateway-response-body [^ISeq body]
    {:body            (string/join (into [] body))
     :base64-encoded? false})

  nil
  (->api-gateway-response-body [_]
    {:body            nil
     :base64-encoded? false}))

(defn api-gateway-request->ring-request
  [event context]
  {:server-name    (events/server-name event)
   :server-port    (events/server-port event)
   :remote-addr    (events/remote-addr event)
   :uri            (events/uri event)
   :query-string   (events/query-string event)
   :scheme         (events/scheme event)
   :request-method (events/request-method event)
   :headers        (events/headers event)
   :protocol       (events/protocol event)
   :body           (events/body event)
   :lambda         {:event   event
                    :context context}})

(defn ring-response->api-gateway-response
  ([ring-response]
   (ring-response->api-gateway-response ring-response {}))
  ([{:keys [status body headers]}
    {:keys [version]
     :or   {version "1.0"}}]
   (let [{:keys [body base64-encoded?]}
         (protocols/->api-gateway-response-body body)
         headers (utils/transform-keys-shallow
                   csk-core/->kebab-case-string headers)
         [other-headers set-cookie-headers]
         (utils/split-map headers ["set-cookie"])
         other-headers
         (update-vals other-headers
           (fn [val] (if (string? val) val (string/join ", " val))))
         cookies-response
         (if (= version "2.0")
           {:cookies (get set-cookie-headers "set-cookie")}
           {:multi-value-headers set-cookie-headers})]
     (merge cookies-response
       {:status-code     status
        :body            body
        :base64-encoded? base64-encoded?
        :headers         other-headers}))))
