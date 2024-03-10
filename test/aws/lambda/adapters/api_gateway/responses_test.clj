(ns aws.lambda.adapters.api-gateway.responses-test
  (:require
   [clojure.test :refer [deftest is]]

   [aws.lambda.adapters.api-gateway.responses :as responses]))

(deftest
  normalise-response-converts-response-keys-to-correct-camel-case-strings-v1
  (let [response {:status-code         200
                  :headers             {"content-type"     "application-json"
                                        "content-encoding" "gzip"}
                  :body                "Hello world!"
                  :base64-encoded?     false
                  :multi-value-headers {"set-cookie" ["k1=v1", "k2=v2"]}}
        normalised (responses/normalise-response response)]
    (is (= {"statusCode" 200
            "headers"    {"content-type"     "application-json"
                          "content-encoding" "gzip"}
            "body"       "Hello world!"
            "isBase64Encoded" false
            "multiValueHeaders" {"set-cookie" ["k1=v1", "k2=v2"]}}
          normalised))))
