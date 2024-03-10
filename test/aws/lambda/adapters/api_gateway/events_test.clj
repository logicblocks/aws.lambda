(ns aws.lambda.adapters.api-gateway.events-test
  (:require
   [clojure.test :refer [deftest is]]

   [aws.lambda.adapters.api-gateway.events :as events]
   [aws.lambda.adapters.test-support.data :as data]
   [aws.lambda.adapters.test-support.predicates :as predicates]))

;; normalise v1 api gateway events
(deftest
  normalise-event-converts-top-level-keys-to-kebab-case-keywords-v1
  (let [raw-event (data/raw-api-gateway-v1-event)
        normalised-event (events/normalise-event raw-event)]
    (is (every? predicates/kebab-case-keyword? (keys normalised-event)))))

(deftest
  normalise-event-converts-request-context-keys-to-kebab-case-keywords-v1
  (let [raw-event (data/raw-api-gateway-v1-event)
        normalised-event (events/normalise-event raw-event)
        field (:request-context normalised-event)]
    (is (not (nil? field)))
    (is (every? predicates/kebab-case-keyword? (keys field)))))

(deftest normalise-event-converts-identity-keys-to-kebab-case-keywords-v1
  (let [raw-event (data/raw-api-gateway-v1-event)
        normalised-event (events/normalise-event raw-event)
        field (get-in normalised-event [:request-context :identity])]
    (is (not (nil? field)))
    (is (every? predicates/kebab-case-keyword? (keys field)))))

(deftest normalise-event-converts-client-cert-keys-to-kebab-case-keywords-v1
  (let [raw-event (data/raw-api-gateway-v1-event)
        normalised-event (events/normalise-event raw-event)
        field (get-in normalised-event
                [:request-context :identity :client-cert])]
    (is (not (nil? field)))
    (is (every? predicates/kebab-case-keyword? (keys field)))))

(deftest
  normalise-event-converts-client-cert-validity-keys-to-kebab-case-keywords-v1
  (let [raw-event (data/raw-api-gateway-v1-event)
        normalised-event (events/normalise-event raw-event)
        field (get-in normalised-event
                [:request-context :identity :client-cert :validity])]
    (is (not (nil? field)))
    (is (every? predicates/kebab-case-keyword? (keys field)))))

(deftest normalise-event-converts-authorizer-keys-to-kebab-case-keywords-v1
  (let [raw-event (data/raw-api-gateway-v1-event)
        normalised-event (events/normalise-event raw-event)
        field (get-in normalised-event [:request-context :authorizer])]
    (is (not (nil? field)))
    (is (every? predicates/kebab-case-keyword? (keys field)))))

(deftest normalise-event-leaves-stage-variables-as-camel-case-strings-v1
  (let [raw-event (data/raw-api-gateway-v1-event
                    {:stage-variables
                     {"firstStageVariable"  "value1"
                      "secondStageVariable" "value2"}})
        normalised-event (events/normalise-event raw-event)
        field (get normalised-event :stage-variables)]
    (is (not (nil? field)))
    (is (every? predicates/camel-case-string? (keys field)))))

(deftest normalise-event-leaves-path-params-as-camelcase-strings-v1
  (let [raw-event (data/raw-api-gateway-v1-event
                    {:path-parameters
                     {"firstPathParam"  "value1"
                      "secondPathParam" "value2"}})
        normalised-event (events/normalise-event raw-event)
        field (get normalised-event :path-parameters)]
    (is (not (nil? field)))
    (is (every? predicates/camel-case-string? (keys field)))))

(deftest normalise-event-leaves-query-string-params-as-camelcase-strings-v1
  (let [raw-event (data/raw-api-gateway-v1-event
                    {:query-string-parameters
                     {"firstQueryParam"  "value1a,value1b"
                      "secondQueryParam" "value2"}})
        normalised-event (events/normalise-event raw-event)
        field (get normalised-event :query-string-parameters)]
    (is (not (nil? field)))
    (is (every? predicates/camel-case-string? (keys field)))))

(deftest
  normalise-event-leaves-multi-value-query-string-params-as-camelcase-strings-v1
  (let [raw-event (data/raw-api-gateway-v1-event
                    {:multi-value-query-string-parameters
                     {"firstQueryParam"  ["value1a" "value1b"]
                      "secondQueryParam" ["value2"]}})
        normalised-event (events/normalise-event raw-event)
        field (get normalised-event :multi-value-query-string-parameters)]
    (is (not (nil? field)))
    (is (every? predicates/camel-case-string? (keys field)))))

(deftest
  normalise-event-converts-header-names-to-lower-case-strings-v1
  (let [raw-event (data/raw-api-gateway-v1-event
                    {:headers
                     {"Host"             "12345678.example.com"
                      "X-Special-Header" "value"}})
        normalised-event (events/normalise-event raw-event)
        field (get normalised-event :headers)]
    (is (not (nil? field)))
    (is (every? predicates/kebab-case-string? (keys field)))))

(deftest
  normalise-event-converts-multi-value-header-names-to-lower-case-strings-v1
  (let [raw-event (data/raw-api-gateway-v1-event
                    {:multi-value-headers
                     {"Host"             ["12345678.example.com"]
                      "X-Special-Header" ["value1" "value2"]}})
        normalised-event (events/normalise-event raw-event)
        field (get normalised-event :headers)]
    (is (not (nil? field)))
    (is (every? predicates/kebab-case-string? (keys field)))))

(deftest normalise-event-renames-is-base-64-encoded-to-base64-encoded?-v1
  (let [raw-event (data/raw-api-gateway-v1-event)
        normalised-event (events/normalise-event raw-event)]
    (is (nil? (get normalised-event :is-base-64-encoded)))
    (is (boolean? (get normalised-event :base64-encoded?)))))

;; normalise v2 api gateway events
(deftest normalise-event-converts-top-level-keys-to-kebab-case-keywords-v2
  (let [raw-event (data/raw-api-gateway-v2-event)
        normalised-event (events/normalise-event raw-event)]
    (is (every? predicates/kebab-case-keyword? (keys normalised-event)))))

(deftest
  normalise-event-converts-request-context-keys-to-kebab-case-keywords-v2
  (let [raw-event (data/raw-api-gateway-v2-event)
        normalised-event (events/normalise-event raw-event)
        field (:request-context normalised-event)]
    (is (not (nil? field)))
    (is (every? predicates/kebab-case-keyword? (keys field)))))

(deftest normalise-event-converts-client-cert-keys-to-kebab-case-keywords-v2
  (let [raw-event (data/raw-api-gateway-v2-event)
        normalised-event (events/normalise-event raw-event)
        field (get-in normalised-event
                [:request-context :authentication :client-cert])]
    (is (not (nil? field)))
    (is (every? predicates/kebab-case-keyword? (keys field)))))

(deftest
  normalise-event-converts-client-cert-validity-keys-to-kebab-case-keywords-v2
  (let [raw-event (data/raw-api-gateway-v2-event)
        normalised-event (events/normalise-event raw-event)
        field (get-in normalised-event
                [:request-context :authentication :client-cert :validity])]
    (is (not (nil? field)))
    (is (every? predicates/kebab-case-keyword? (keys field)))))

(deftest normalise-event-converts-authorizer-keys-to-kebab-case-keywords-v2
  (let [raw-event (data/raw-api-gateway-v2-event)
        normalised-event (events/normalise-event raw-event)
        field (get-in normalised-event [:request-context :authorizer :jwt])]
    (is (not (nil? field)))
    (is (every? predicates/kebab-case-keyword? (keys field)))))

(deftest normalise-event-converts-http-keys-to-kebab-case-keywords-v1
  (let [raw-event (data/raw-api-gateway-v2-event)
        normalised-event (events/normalise-event raw-event)
        field (get-in normalised-event [:request-context :http])]
    (is (not (nil? field)))
    (is (every? predicates/kebab-case-keyword? (keys field)))))

(deftest normalise-event-leaves-stage-variables-as-camelcase-strings-v2
  (let [raw-event (data/raw-api-gateway-v2-event
                    {:stage-variables
                     {"firstStageVariable"  "value1"
                      "secondStageVariable" "value2"}})
        normalised-event (events/normalise-event raw-event)
        field (get normalised-event :stage-variables)]
    (is (not (nil? field)))
    (is (every? predicates/camel-case-string? (keys field)))))

(deftest normalise-event-leaves-path-params-as-camelcase-strings-v2
  (let [raw-event (data/raw-api-gateway-v2-event
                    {:path-parameters
                     {"firstPathParam"  "value1"
                      "secondPathParam" "value2"}})
        normalised-event (events/normalise-event raw-event)
        field (get normalised-event :path-parameters)]
    (is (not (nil? field)))
    (is (every? predicates/camel-case-string? (keys field)))))

(deftest normalise-event-leaves-query-string-params-as-camelcase-strings-v2
  (let [raw-event (data/raw-api-gateway-v2-event
                    {:query-string-parameters
                     {"firstQueryParam"  "value1a,value1b"
                      "secondQueryParam" "value2"}})
        normalised-event (events/normalise-event raw-event)
        field (get normalised-event :query-string-parameters)]
    (is (not (nil? field)))
    (is (every? predicates/camel-case-string? (keys field)))))

(deftest
  normalise-event-converts-header-names-to-lower-case-strings-v2
  (let [raw-event (data/raw-api-gateway-v2-event
                    {:headers
                     {"Host"             "12345678.example.com"
                      "X-Special-Header" "value"}})
        normalised-event (events/normalise-event raw-event)
        field (get normalised-event :headers)]
    (is (not (nil? field)))
    (is (every? predicates/kebab-case-string? (keys field)))))

(deftest normalise-event-renames-is-base-64-encoded-to-base64-encoded?-v2
  (let [raw-event (data/raw-api-gateway-v2-event)
        normalised-event (events/normalise-event raw-event)]
    (is (false? (contains? normalised-event :is-base-64-encoded)))
    (is (boolean? (get normalised-event :base64-encoded?)))))

(deftest
  normalise-event-does-not-include-multi-value-query-string-parameters-v2
  (let [raw-event (data/raw-api-gateway-v2-event)
        normalised-event (events/normalise-event raw-event)]
    (is (false? (contains? normalised-event
                  :multi-value-query-string-parameters)))))

(deftest
  normalise-event-does-not-include-multi-value-headers-v2
  (let [raw-event (data/raw-api-gateway-v2-event)
        normalised-event (events/normalise-event raw-event)]
    (is (false? (contains? normalised-event
                  :multi-value-headers)))))
