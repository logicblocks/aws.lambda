(ns aws.lambda.adapters.api-gateway.responses
  (:require
   [clojure.set :as set]

   [camel-snake-kebab.core :as csk-core]

   [aws.lambda.adapters.utils :as utils]))

(defn normalise-response [response]
  (utils/transform-keys-shallow
    csk-core/->camelCaseString
    (set/rename-keys response {:base64-encoded? :is-base64-encoded})))
