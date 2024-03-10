(ns aws.lambda.adapters.api-gateway.events
  (:require
   [clojure.string :as string]

   [camel-snake-kebab.core :as csk-core]

   [ring.util.codec :as codec]

   [aws.lambda.adapters.utils :as utils]
   [aws.lambda.adapters.api-gateway.protocols :as protocols]))

(defn server-name [event]
  (or
    (utils/drill-> event :headers #(get % "via")
      (fn [val] (second (string/split val #" "))))
    (utils/drill-> event :request-context :http :source-ip)
    (utils/drill-> event :request-context :identity :source-ip)))

(defn server-port [event]
  (utils/drill-> event :headers
    #(get % "x-forwarded-port")
    utils/integer-or-nil))

(defn remote-addr [event]
  (or
    (utils/drill-> event :request-context :http :source-ip)
    (utils/drill-> event :request-context :identity :source-ip)))

(defn uri [event]
  (or
    (utils/drill-> event :request-context :http :path)
    (utils/drill-> event :path)))

(defn query-string [event]
  (or
    (utils/drill-> event :raw-query-string)
    (utils/drill-> event :query-string-parameters codec/form-encode)))

(defn scheme [event]
  (utils/drill-> event :headers #(get % "x-forwarded-proto") keyword))

(defn request-method [event]
  (utils/drill->
    (or
      (utils/drill-> event :request-context :http :method)
      (utils/drill-> event :http-method))
    string/lower-case
    keyword))

(defn protocol [event]
  (or
    (utils/drill-> event :request-context :http :protocol)
    (utils/drill-> event :request-context :protocol)))

(defn headers [event]
  (utils/drill-> event :headers))

(defn body [event]
  (let [body (utils/drill-> event :body)
        base64-encoded? (utils/drill-> event :base64-encoded?)]
    (protocols/->ring-request-body body base64-encoded?)))

(defmulti normalise-event #(get % "version" "1.0"))

(defmethod normalise-event "1.0" [event]
  (let [special-case-entries
        [:stage-variables
         :path-parameters
         :query-string-parameters
         :multi-value-query-string-parameters
         :headers
         :multi-value-headers
         :is-base-64-encoded]

        [event
         {:keys [stage-variables
                 path-parameters
                 query-string-parameters
                 multi-value-query-string-parameters
                 headers
                 multi-value-headers
                 is-base-64-encoded]}]
        (utils/split-map
          (utils/transform-keys-shallow
            csk-core/->kebab-case-keyword
            event)
          special-case-entries)

        event
        (utils/transform-keys-deep
          csk-core/->kebab-case-keyword
          event)

        headers
        (utils/transform-keys-shallow
          csk-core/->kebab-case-string
          headers)
        multi-value-headers
        (utils/transform-keys-shallow
          csk-core/->kebab-case-string
          multi-value-headers)]
    (assoc event
      :stage-variables stage-variables
      :path-parameters path-parameters
      :query-string-parameters query-string-parameters
      :multi-value-query-string-parameters multi-value-query-string-parameters
      :headers headers
      :multi-value-headers multi-value-headers
      :base64-encoded? is-base-64-encoded)))

(defmethod normalise-event "2.0" [event]
  (let [special-case-entries
        [:stage-variables
         :path-parameters
         :query-string-parameters
         :headers
         :is-base-64-encoded]

        [event
         {:keys [stage-variables
                 path-parameters
                 query-string-parameters
                 headers
                 is-base-64-encoded]}]
        (utils/split-map
          (utils/transform-keys-shallow
            csk-core/->kebab-case-keyword
            event)
          special-case-entries)

        event
        (utils/transform-keys-deep
          csk-core/->kebab-case-keyword
          event)

        headers
        (utils/transform-keys-shallow
          csk-core/->kebab-case-string
          headers)]
    (assoc event
      :stage-variables stage-variables
      :path-parameters path-parameters
      :query-string-parameters query-string-parameters
      :headers headers
      :base64-encoded? is-base-64-encoded)))
