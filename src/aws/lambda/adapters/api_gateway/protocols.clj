(ns aws.lambda.adapters.api-gateway.protocols)

(defprotocol RequestBodyTransformer
  (->ring-request-body [body base64-encoded?]))

(defprotocol ResponseBodyTransformer
  (->api-gateway-response-body [body]))
