(ns aws.lambda.adapters.test-support.data
  (:require
   [clojure.string :as string]

   [ring.util.codec :as codec]

   [faker.lorem :as fl])
  (:import
   [com.amazonaws.services.lambda.runtime Context]
   [java.time Instant ZoneId]
   [java.time.format DateTimeFormatter]))

(defn- ->single-valued [val] (if (vector? val) (string/join "," val) val))
(defn- ->multi-valued [val] (if (vector? val) val [val]))

(def ^DateTimeFormatter
  api-gateway-request-context-time-formatter
  (-> (DateTimeFormatter/ofPattern
        "dd'/'LLL'/'yyyy':'HH':'mm':'ss' 'xxxx")
    (.withZone (ZoneId/systemDefault))))

(def random-digit-seq
  (repeatedly (partial rand-nth (shuffle (range 10)))))

(def random-hex-seq
  (let [characters
        (into []
          (concat
            (map (comp str char) (range 97 103))
            (map (comp str char) (range 48 58))))]
    (repeatedly (partial rand-nth (shuffle characters)))))

(def random-alphabetic-seq
  (let [characters
        (into []
          (concat
            (map (comp str char) (range 97 123))
            (map (comp str char) (range 65 91))))]
    (repeatedly (partial rand-nth (shuffle characters)))))

(def random-lower-alphanumeric-seq
  (let [characters
        (into []
          (concat
            (map (comp str char) (range 97 123))
            (map (comp str char) (range 48 58))))]
    (repeatedly (partial rand-nth (shuffle characters)))))

(defn random-boolean []
  (rand-nth [false true]))

(defn random-word []
  (first (fl/words)))

(defn random-account-id []
  (let [digits (take 10 random-digit-seq)]
    (string/join digits)))

(defn random-region []
  (rand-nth ["us-east-1" "eu-west-1" "eu-west-2"]))

(defn random-api-id []
  (let [characters (take 10 random-lower-alphanumeric-seq)]
    (string/join characters)))

(defn random-api-domain-name
  ([] (random-api-domain-name {}))
  ([{:keys [api-id region]
     :or   {api-id (random-api-id)
            region (random-region)}}]
   (str "https://" api-id ".execute-api." region ".amazonaws.com")))

(defn random-protocol []
  (rand-nth ["HTTP/1.1" "HTTP/1.0"]))

(defn random-stage []
  (rand-nth ["dev" "test" "prod"]))

(defn random-stage-variables []
  (let [entries (rand-int 5)
        words (vec (take (* 2 entries) (fl/words)))
        pairs (map vec (partition 2 words))]
    (into {} pairs)))

(defn random-resource-id []
  (let [digits (take 7 random-lower-alphanumeric-seq)]
    (string/join digits)))

(defn random-resource
  ([] (random-resource {:proxy? (random-boolean)}))
  ([{:keys [proxy?]}]
   (if proxy?
     "/{proxy+}"
     (rand-nth ["/", "/a", "/a/b", "/a/b/c", "/a/b/c/d"]))))

(defn random-path
  ([] (random-path {:resource (random-resource)}))
  ([{:keys [resource]}]
   (if (= resource "/{proxy+}")
     (rand-nth ["/", "/a", "/a/b", "/a/b/c", "/a/b/c/d"])
     resource)))

(defn random-full-path
  ([]
   (random-full-path {}))
  ([{:keys [stage path]
     :or   {stage (random-stage)
            path  (random-path)}}]
   (str "/" stage path)))

(defn random-path-parameters
  ([] (random-path-parameters {}))
  ([{:keys [proxy? path]}]
   (if proxy?
     {"proxy" path}
     nil)))

(defn random-http-method []
  (rand-nth ["GET" "PUT" "POST" "PATCH" "DELETE" "HEAD" "OPTIONS"]))

(defn random-route-key
  ([] (random-route-key
        {:http-method (random-http-method)
         :resource    (random-resource)}))
  ([{:keys [http-method resource]}]
   (str http-method " " resource)))

(defn random-cookie
  ([] (random-cookie {}))
  ([{:keys [key value]
     :or   {key   (random-word)
            value (random-word)}}]
   (str key "=" value)))

(defn random-cookies []
  (let [cookie-count (rand-int 5)]
    (if (zero? cookie-count)
      nil
      (into [] (repeatedly cookie-count random-cookie)))))

(defn random-query-string-parameters []
  (let [query-param-count (rand-int 5)
        query-param-pairs
        (vec (repeatedly
               query-param-count
               (fn [] [(first (fl/words))
                       (vec (take (+ 1 (rand-int 3)) (fl/words)))])))]
    (if (zero? query-param-count)
      nil
      (into {} query-param-pairs))))

(defn random-body
  ([] (random-body {}))
  ([{:keys [base64-encoded?]
     :or   {base64-encoded? (random-boolean)}}]
   (let [bodies ["{\"first\": 1, \"second\": 2}"
                 "{\"hello\": \"world\"}"
                 "{}"]
         body
         (if base64-encoded?
           (rand-nth bodies)
           (rand-nth (conj bodies nil)))]
     (if base64-encoded?
       (codec/base64-encode (.getBytes ^String body))
       body))))

(defn random-request-id []
  (str (random-uuid)))

(defn random-request-time
  ([] (random-request-time {}))
  ([{:keys [instant]}]
   (let [instant (or instant (Instant/now))]
     (.format api-gateway-request-context-time-formatter instant))))

(defn random-request-time-epoch
  ([] (random-request-time-epoch {}))
  ([{:keys [instant]}]
   (let [instant (or instant (Instant/now))]
     (.toEpochMilli ^Instant instant))))

(defn random-extended-request-id []
  (str "J" (string/join (take 14 random-alphabetic-seq)) "="))

(defn random-ip-address []
  (let [random-ip-part-seq
        (repeatedly (partial rand-nth (shuffle (range 1 128))))]
    (str
      (nth random-ip-part-seq 0) "."
      (nth random-ip-part-seq 1) "."
      (nth random-ip-part-seq 2) "."
      (nth random-ip-part-seq 3))))

(defn random-user-agent []
  (string/join " " (take 4 (fl/words))))

(defn random-aws-request-id []
  (str (random-uuid)))

(defn random-function-name []
  (string/join "-"
    (map #(string/lower-case %) (take 5 (fl/words)))))

(defn random-function-version []
  "$LATEST")

(defn random-millisecond-duration []
  (rand-int 300000))

(defn random-memory-amount-in-megabytes []
  (rand-int 8192))

(defn random-log-group-name
  ([] (random-log-group-name {}))
  ([{:keys [function-name]
     :or   {function-name (random-function-name)}}]
   (str "/aws/lambda/" function-name)))

(defn random-log-stream-name
  ([] (random-log-stream-name {}))
  ([{:keys [date
            function-version]
     :or   {date             (Instant/now)
            function-version (random-function-version)}}]
   (let [date-string
         (.format
           (-> (DateTimeFormatter/ofPattern "yyyy'/'MM'/'dd")
             (.withZone (ZoneId/systemDefault)))
           date)]
     (str date-string "/[" function-version "]"
       (string/join (take 32 random-hex-seq))))))

(defn random-function-arn
  ([] (random-function-arn {}))
  ([{:keys [account-id
            region
            function-name]
     :or   {account-id    (random-account-id)
            region        (random-region)
            function-name (random-function-name)}}]
   (str "arn:aws:lambda:" region ":" account-id ":function:" function-name)))

(defn random-cognito-identity-id []
  (str (random-uuid)))

(defn random-cognito-identity-pool-id
  ([] (random-cognito-identity-pool-id {}))
  ([{:keys [region]
     :or   {region (random-region)}}]
   (str region ":" (random-uuid))))

(defn random-client []
  (let [installation-id (str (random-uuid))
        app-title (string/join "-" (take 3 (fl/words)))
        app-version-name (rand-nth ["1.0" "1.1" "2.0" "3.0"])
        app-version-code (rand-int 200)
        app-package-name (string/join "." (take 4 (fl/words)))]
    {:installation-id  installation-id
     :app-title        app-title
     :app-version-name app-version-name
     :app-version-code app-version-code
     :app-package-name app-package-name}))

(defn random-client-custom-values []
  (let [entry-count (rand-int 5)]
    (when (> entry-count 0)
      (into {}
        (vec (repeatedly
               entry-count
               (fn [] [(first (fl/words))
                       (second (fl/words))])))))))

(defn random-client-environment []
  (let [entry-count (rand-int 5)]
    (when (> entry-count 0)
      (into {}
        (vec (repeatedly
               entry-count
               (fn [] [(string/upper-case (first (fl/words)))
                       (second (fl/words))])))))))

(defn via-header-value [{:keys [protocol-version host comment]}]
  (let [via [protocol-version host]
        via (if comment (conj via (str "(" comment ")")) via)]
    (string/join " " via)))

(defn normalised-lambda-context-cognito-identity
  ([] (normalised-lambda-context-cognito-identity {}))
  ([{:keys [id
            pool-id]
     :or   {id      (random-cognito-identity-id)
            pool-id (random-cognito-identity-pool-id)}}]
   {:identity-id      id
    :identity-pool-id pool-id}))

(defn normalised-lambda-context-client-context
  ([] (normalised-lambda-context-client-context {}))
  ([overrides]
   (let [custom
         (get overrides :custom
           (random-client-custom-values))
         environment
         (get overrides :environment
           (random-client-environment))
         client
         (get overrides :client
           (random-client))]
     {:custom      custom
      :environment environment
      :client      client})))

(defn normalised-lambda-context
  ([] (normalised-lambda-context {}))
  ([{:keys [aws-request-id
            function-name
            function-version
            remaining-time-in-millis
            memory-limit-in-mb
            identity
            client-context]
     :or   {aws-request-id           (random-aws-request-id)
            function-name            (random-function-name)
            function-version         (random-function-version)
            remaining-time-in-millis (random-millisecond-duration)
            memory-limit-in-mb       (random-memory-amount-in-megabytes)
            identity
            (normalised-lambda-context-cognito-identity)
            client-context
            (normalised-lambda-context-client-context)}
     :as   overrides}]
   (let [log-group-name
         (get overrides :log-group-name
           (random-log-group-name {:function-name function-name}))
         log-stream-name
         (get overrides :log-stream-name
           (random-log-stream-name {:function-version function-version}))
         invoked-function-arn
         (get overrides :invoked-function-arn
           (random-function-arn
             {:function-name function-name}))]
     {:aws-request-id           aws-request-id
      :log-group-name           log-group-name
      :log-stream-name          log-stream-name
      :function-name            function-name
      :function-version         function-version
      :invoked-function-arn     invoked-function-arn
      :remaining-time-in-millis remaining-time-in-millis
      :memory-limit-in-mb       memory-limit-in-mb
      :identity                 identity
      :client-context           client-context})))

(defn raw-lambda-context
  ([] (raw-lambda-context {}))
  ([{:keys [aws-request-id
            function-name
            function-version
            remaining-time-in-millis
            memory-limit-in-mb]
     :or   {aws-request-id           (random-aws-request-id)
            function-name            (random-function-name)
            function-version         (random-function-version)
            remaining-time-in-millis (random-millisecond-duration)
            memory-limit-in-mb       (random-memory-amount-in-megabytes)}
     :as   overrides}]
   (let [log-group-name
         (get overrides :log-group-name
           (random-log-group-name {:function-name function-name}))
         log-stream-name
         (get overrides :log-stream-name
           (random-log-stream-name {:function-version function-version}))
         invoked-function-arn
         (get overrides :invoked-function-arn
           (random-function-arn
             {:function-name function-name}))]
     (reify Context
       (getAwsRequestId [_] aws-request-id)
       (getLogGroupName [_] log-group-name)
       (getLogStreamName [_] log-stream-name)
       (getFunctionName [_] function-name)
       (getFunctionVersion [_] function-version)
       (getInvokedFunctionArn [_] invoked-function-arn)
       (getRemainingTimeInMillis [_] remaining-time-in-millis)
       (getMemoryLimitInMB [_] memory-limit-in-mb)
       (getIdentity [_] nil)
       (getClientContext [_] nil)))))

(defn normalised-api-gateway-token-contents
  ([] (normalised-api-gateway-token-contents {}))
  ([{:keys [claims
            scopes]}]
   {:claims claims
    :scopes scopes}))

(defn raw-api-gateway-token-contents
  ([] (raw-api-gateway-token-contents {}))
  ([{:keys [claims
            scopes]}]
   {"claims" claims
    "scopes" scopes}))

(defn normalised-api-gateway-client-certificate
  ([] (normalised-api-gateway-client-certificate {}))
  ([overrides]
   (let [client-cert-pem (get overrides :client-cert-pem "CERT_CONTENT")
         subject-dn (get overrides :subject-dn "www.example.com")
         issuer-dn (get overrides :issuer-dn "Example issuer")
         serial-number
         (get overrides :serial-number
           "a1:a1:a1:a1:a1:a1:a1:a1:a1:a1:a1:a1:a1:a1:a1:a1")
         validity-not-before
         (get overrides :validity-not-before
           "May 28 12:30:02 2019 GMT")
         validity-not-after
         (get overrides :validity-not-after
           "Aug  5 09:36:04 2028 GMT")]
     {:client-cert-pem client-cert-pem
      :subject-dn      subject-dn
      :issuer-dn       issuer-dn
      :serial-number   serial-number
      :validity
      {:not-before validity-not-before
       :not-after  validity-not-after}})))

(defn raw-api-gateway-client-certificate
  ([] (raw-api-gateway-client-certificate {}))
  ([overrides]
   (let [client-cert-pem (get overrides :client-cert-pem "CERT_CONTENT")
         subject-dn (get overrides :subject-dn "www.example.com")
         issuer-dn (get overrides :issuer-dn "Example issuer")
         serial-number
         (get overrides :serial-number
           "a1:a1:a1:a1:a1:a1:a1:a1:a1:a1:a1:a1:a1:a1:a1:a1")
         validity-not-before
         (get overrides :validity-not-before
           "May 28 12:30:02 2019 GMT")
         validity-not-after
         (get overrides :validity-not-after
           "Aug  5 09:36:04 2028 GMT")]
     {"clientCertPem" client-cert-pem
      "subjectDN"     subject-dn
      "issuerDN"      issuer-dn
      "serialNumber"  serial-number
      "validity"
      {"notBefore" validity-not-before
       "notAfter"  validity-not-after}})))

(defn normalised-api-gateway-v1-request-identity
  ([] (normalised-api-gateway-v1-request-identity {}))
  ([{:keys [access-key
            account-id
            caller
            cognito-authentication-provider
            cognito-authentication-type
            cognito-identity-id
            cognito-identity-pool-id
            principal-org-id
            user
            user-arn]
     :as   overrides}]
   (let [source-ip (get overrides :source-ip (random-ip-address))
         user-agent (get overrides :user-agent (random-user-agent))

         client-cert
         (get overrides :client-cert
           (normalised-api-gateway-client-certificate))]
     {:access-key                      access-key
      :account-id                      account-id
      :caller                          caller
      :cognito-authentication-provider cognito-authentication-provider
      :cognito-authentication-type     cognito-authentication-type
      :cognito-identity-id             cognito-identity-id
      :cognito-identity-pool-id        cognito-identity-pool-id
      :principal-org-id                principal-org-id
      :source-ip                       source-ip
      :user                            user
      :user-agent                      user-agent
      :user-arn                        user-arn
      :client-cert                     client-cert})))

(defn raw-api-gateway-v1-request-identity
  ([] (raw-api-gateway-v1-request-identity {}))
  ([{:keys [access-key
            account-id
            caller
            cognito-authentication-provider
            cognito-authentication-type
            cognito-identity-id
            cognito-identity-pool-id
            principal-org-id
            user
            user-arn]
     :as   overrides}]
   (let [source-ip (get overrides :source-ip (random-ip-address))
         user-agent (get overrides :user-agent (random-user-agent))

         client-cert
         (get overrides :client-cert
           (raw-api-gateway-client-certificate))]
     {"accessKey"                     access-key
      "accountId"                     account-id
      "caller"                        caller
      "cognitoAuthenticationProvider" cognito-authentication-provider
      "cognitoAuthenticationType"     cognito-authentication-type
      "cognitoIdentityId"             cognito-identity-id
      "cognitoIdentityPoolId"         cognito-identity-pool-id
      "principalOrgId"                principal-org-id
      "sourceIp"                      source-ip
      "user"                          user
      "userAgent"                     user-agent
      "userArn"                       user-arn
      "clientCert"                    client-cert})))

(defn api-gateway-v1-request-context-data [overrides]
  (let [now (Instant/now)

        {:keys [proxy?
                request-time
                request-time-epoch
                resource-id
                account-id
                api-id
                http-method
                protocol
                stage
                request-id
                extended-request-id
                authorizer
                identity]
         :or   {request-time        (random-request-time {:instant now})
                request-time-epoch  (random-request-time-epoch {:instant now})
                resource-id         (random-resource-id)
                account-id          (random-account-id)
                api-id              (random-api-id)
                http-method         (random-http-method)
                protocol            (random-protocol)
                stage               (random-stage)
                request-id          (random-request-id)
                extended-request-id (random-extended-request-id)
                authorizer          (raw-api-gateway-token-contents)
                identity            (raw-api-gateway-v1-request-identity)}}
        overrides

        resource-path
        (get overrides :resource-path
          (random-resource {:proxy? proxy?}))
        domain-name
        (get overrides :domain-name
          (random-api-domain-name {:api-id api-id}))
        domain-prefix
        (get overrides :domain-prefix api-id)
        path
        (get overrides :path
          (random-full-path {:stage stage :path resource-path}))]
    {:account-id          account-id
     :api-id              api-id
     :domain-name         domain-name
     :domain-prefix       domain-prefix
     :http-method         http-method
     :path                path
     :protocol            protocol
     :resource-id         resource-id
     :resource-path       resource-path
     :stage               stage
     :request-id          request-id
     :extended-request-id extended-request-id
     :request-time-epoch  request-time-epoch
     :request-time        request-time
     :authorizer          authorizer
     :identity            identity}))

(defn normalised-api-gateway-v1-request-context
  ([] (normalised-api-gateway-v1-request-context {}))
  ([overrides]
   (api-gateway-v1-request-context-data overrides)))

(defn raw-api-gateway-v1-request-context
  ([] (raw-api-gateway-v1-request-context) {})
  ([overrides]
   (let [{:keys [account-id
                 api-id
                 domain-name
                 domain-prefix
                 http-method
                 path
                 protocol
                 resource-id
                 resource-path
                 stage
                 request-id
                 extended-request-id
                 request-time-epoch
                 request-time
                 authorizer
                 identity]}
         (api-gateway-v1-request-context-data overrides)]
     {"accountId"         account-id
      "apiId"             api-id
      "domainName"        domain-name
      "domainPrefix"      domain-prefix
      "httpMethod"        http-method
      "path"              path
      "protocol"          protocol
      "resourceId"        resource-id
      "resourcePath"      resource-path
      "stage"             stage
      "requestId"         request-id
      "extendedRequestId" extended-request-id
      "requestTimeEpoch"  request-time-epoch
      "requestTime"       request-time
      "authorizer"        authorizer
      "identity"          identity})))

(defn api-gateway-v1-event-data [overrides]
  (let [{:keys [proxy?
                base64-encoded?
                stage-variables
                http-method
                query-string-parameters
                headers]
         :or   {proxy?                  (random-boolean)
                base64-encoded?         (random-boolean)
                stage-variables         (random-stage-variables)
                http-method             (random-http-method)
                query-string-parameters (random-query-string-parameters)
                headers                 {}}}
        overrides

        resource
        (get overrides :resource
          (random-resource {:proxy? proxy?}))
        path
        (get overrides :path
          (random-path {:resource resource}))
        path-parameters
        (get overrides :path-parameters
          (random-path-parameters {:proxy? proxy? :path path}))
        body
        (get overrides :body
          (random-body {:base64-encoded? base64-encoded?}))

        single-value-query-string-parameters
        (get overrides :single-value-query-string-parameters
          (when query-string-parameters
            (update-vals query-string-parameters ->single-valued)))
        multi-value-query-string-parameters
        (get overrides :multi-value-query-string-parameters
          (when query-string-parameters
            (update-vals query-string-parameters ->multi-valued)))

        single-value-headers (update-vals headers ->single-valued)
        multi-value-headers (update-vals headers ->multi-valued)]
    {:resource                            resource
     :stage-variables                     stage-variables
     :http-method                         http-method
     :path                                path
     :path-parameters                     path-parameters
     :query-string-parameters             single-value-query-string-parameters
     :multi-value-query-string-parameters multi-value-query-string-parameters
     :body                                body
     :base64-encoded?                     base64-encoded?
     :headers                             single-value-headers
     :multi-value-headers                 multi-value-headers}))

(defn normalised-api-gateway-v1-event
  ([] (normalised-api-gateway-v1-event {}))
  ([overrides]
   (let [proxy? (get overrides :proxy? (random-boolean))
         host (get overrides :host
                (random-api-domain-name {:api-id (random-api-id)}))
         headers (merge {"host" host}
                   (get overrides :headers {}))

         data (api-gateway-v1-event-data
                (merge overrides
                  {:proxy?  proxy?
                   :headers headers}))

         stage (random-stage)

         request-context
         (get overrides :request-context
           (normalised-api-gateway-v1-request-context
             {:proxy?        proxy?
              :api-id        (:api-id data)
              :domain-name   host
              :stage         stage
              :resource-path (:resource data)
              :path          (random-full-path
                               {:stage stage :path (:path data)})}))]
     (assoc data :request-context request-context))))

(defn raw-api-gateway-v1-event
  ([] (raw-api-gateway-v1-event {}))
  ([overrides]
   (let [proxy? (get overrides :proxy? (random-boolean))
         stage (random-stage)
         api-id (random-api-id)
         host (get overrides :host
                (random-api-domain-name {:api-id api-id}))
         headers (merge {"Host" host}
                   (get overrides :headers {}))

         {:keys [resource
                 stage-variables
                 http-method
                 path
                 path-parameters
                 query-string-parameters
                 multi-value-query-string-parameters
                 body
                 base64-encoded?
                 single-value-headers
                 multi-value-headers]}
         (api-gateway-v1-event-data
           (merge overrides
             {:proxy?  proxy?
              :headers headers}))

         request-context
         (get overrides :request-context
           (normalised-api-gateway-v1-request-context
             {:proxy?        proxy?
              :api-id        api-id
              :domain-name   host
              :stage         stage
              :resource-path resource
              :path          (random-full-path {:stage stage :path path})}))]
     {"resource"                        resource
      "stageVariables"                  stage-variables
      "httpMethod"                      http-method
      "path"                            path
      "pathParameters"                  path-parameters
      "queryStringParameters"           query-string-parameters
      "multiValueQueryStringParameters" multi-value-query-string-parameters
      "body"                            body
      "isBase64Encoded"                 base64-encoded?
      "headers"                         single-value-headers
      "multiValueHeaders"               multi-value-headers
      "requestContext"                  request-context})))

(defn normalised-api-gateway-v2-authentication
  ([] (normalised-api-gateway-v2-authentication {}))
  ([overrides]
   (let [client-cert
         (get overrides :client-cert
           (normalised-api-gateway-client-certificate))]
     {:client-cert client-cert})))

(defn raw-api-gateway-v2-authentication
  ([] (raw-api-gateway-v2-authentication {}))
  ([overrides]
   (let [client-cert
         (get overrides :client-cert
           (raw-api-gateway-client-certificate))]
     {"clientCert" client-cert})))

(defn normalised-api-gateway-v2-authorizer
  ([] (normalised-api-gateway-v2-authorizer {}))
  ([overrides]
   (let [token-contents
         (get overrides :token-content
           (normalised-api-gateway-token-contents))]
     {:jwt token-contents})))

(defn raw-api-gateway-v2-authorizer
  ([] (raw-api-gateway-v2-authorizer {}))
  ([overrides]
   (let [token-contents
         (get overrides :token-content
           (raw-api-gateway-token-contents))]
     {"jwt" token-contents})))

(defn normalised-api-gateway-v2-http-context
  ([] (normalised-api-gateway-v2-http-context {}))
  ([{:keys [method
            path
            protocol
            source-ip
            user-agent]
     :or   {method     (random-http-method)
            path       (random-path)
            protocol   (random-protocol)
            source-ip  (random-ip-address)
            user-agent (random-user-agent)}}]
   {:method     method,
    :path       path,
    :protocol   protocol,
    :source-ip  source-ip,
    :user-agent user-agent}))

(defn raw-api-gateway-v2-http-context
  ([] (raw-api-gateway-v2-http-context {}))
  ([{:keys [method
            path
            protocol
            source-ip
            user-agent]
     :or   {method     (random-http-method)
            path       (random-path)
            protocol   (random-protocol)
            source-ip  (random-ip-address)
            user-agent (random-user-agent)}}]
   {"method"    method,
    "path"      path,
    "protocol"  protocol,
    "sourceIp"  source-ip,
    "userAgent" user-agent}))

(defn api-gateway-v2-request-context-data
  [overrides]
  (let [now (Instant/now)

        {:keys [proxy?
                account-id
                api-id
                request-id
                stage
                time
                time-epoch]
         :or   {account-id (random-account-id)
                api-id     (random-api-id)
                request-id (random-extended-request-id)
                stage      (random-stage)
                time       (random-request-time {:instant now})
                time-epoch (random-request-time-epoch {:instant now})}}
        overrides

        domain-name
        (get overrides :domain-name
          (random-api-domain-name {:api-id api-id}))
        domain-prefix
        (get overrides :domain-prefix api-id)
        route-key
        (get overrides :route-key
          (random-route-key
            {:http-method (random-http-method)
             :resource    (random-resource {:proxy? proxy?})}))]
    {:account-id    account-id
     :api-id        api-id
     :domain-name   domain-name
     :domain-prefix domain-prefix
     :request-id    request-id
     :route-key     route-key
     :stage         stage
     :time          time
     :time-epoch    time-epoch}))

(defn normalised-api-gateway-v2-request-context
  ([] (normalised-api-gateway-v2-request-context {}))
  ([{:keys [http-method
            path]
     :as   overrides}]
   (let [data (api-gateway-v2-request-context-data overrides)

         authentication
         (get overrides :authentication
           (normalised-api-gateway-v2-authentication))

         authorizer
         (get overrides :authorizer
           (normalised-api-gateway-v2-authorizer))
         http
         (get overrides :http
           (normalised-api-gateway-v2-http-context
             {:method http-method
              :path   path}))]
     (merge data
       {:authentication authentication
        :authorizer     authorizer
        :http           http}))))

(defn raw-api-gateway-v2-request-context
  ([] (raw-api-gateway-v2-request-context {}))
  ([{:keys [http-method
            path]
     :as   overrides}]
   (let [{:keys [account-id
                 api-id
                 domain-name
                 domain-prefix
                 request-id
                 route-key
                 stage
                 time
                 time-epoch]}
         (api-gateway-v2-request-context-data overrides)

         authentication
         (get overrides :authentication
           (raw-api-gateway-v2-authentication))
         authorizer
         (get overrides :authorizer
           (raw-api-gateway-v2-authorizer))
         http
         (get overrides :http
           (raw-api-gateway-v2-http-context
             {:method http-method
              :path   path}))]
     {"accountId"      account-id
      "apiId"          api-id
      "authentication" authentication
      "authorizer"     authorizer
      "domainName"     domain-name
      "domainPrefix"   domain-prefix
      "http"           http
      "requestId"      request-id
      "routeKey"       route-key
      "stage"          stage
      "time"           time
      "timeEpoch"      time-epoch})))

(defn api-gateway-v2-event-data
  [{:keys [proxy?
           base64-encoded?
           stage-variables
           http-method
           query-string-parameters
           cookies
           headers]
    :or   {proxy?                  (random-boolean)
           base64-encoded?         (random-boolean)
           stage-variables         (random-stage-variables)
           http-method             (random-http-method)
           query-string-parameters (random-query-string-parameters)
           cookies                 (random-cookies)
           headers                 {}}
    :as   overrides}]
  (let [resource
        (get overrides :resource
          (random-resource {:proxy? proxy?}))
        route-key
        (get overrides :route-key
          (random-route-key
            {:http-method http-method
             :resource    resource}))
        raw-path
        (get overrides :raw-path
          (random-path {:resource resource}))
        path-parameters
        (get overrides :path-parameters
          (random-path-parameters {:proxy? proxy? :path raw-path}))
        body
        (get overrides :body
          (random-body {:base64-encoded? base64-encoded?}))

        single-value-query-string-parameters
        (if (contains? overrides :single-value-query-string-parameters)
          (get overrides :single-value-query-string-parameters)
          (when query-string-parameters
            (update-vals query-string-parameters ->single-valued)))
        multi-value-query-string-parameters
        (if (contains? overrides :multi-value-query-string-parameters)
          (get overrides :multi-value-query-string-parameters)
          (when query-string-parameters
            (update-vals query-string-parameters ->multi-valued)))

        raw-query-string
        (get overrides :raw-query-string
          (when query-string-parameters
            (string/join "&"
              (reduce
                (fn [acc [key coll]]
                  (concat acc (map (fn [value] (str key "=" value)) coll)))
                []
                multi-value-query-string-parameters))))

        headers (update-vals headers ->single-valued)]
    {:version                 "2.0"
     :route-key               route-key
     :raw-path                raw-path
     :raw-query-string        raw-query-string
     :cookies                 cookies
     :headers                 headers
     :query-string-parameters single-value-query-string-parameters
     :body                    body
     :base64-encoded?         base64-encoded?
     :stage-variables         stage-variables
     :path-parameters         path-parameters}))

(defn normalised-api-gateway-v2-event
  ([] (normalised-api-gateway-v2-event {}))
  ([overrides]
   (let [proxy? (get overrides :proxy? (random-boolean))
         stage (random-stage)
         api-id (random-api-id)
         host (get overrides :host
                (random-api-domain-name {:api-id api-id}))
         headers (merge {"host" host}
                   (get overrides :headers {}))

         data (api-gateway-v2-event-data
                (merge overrides
                  {:proxy?  proxy?
                   :headers headers}))

         request-context
         (get overrides :request-context
           (normalised-api-gateway-v2-request-context
             {:proxy?      proxy?
              :api-id      api-id
              :domain-name host
              :route-key   (:route-key data)
              :path        (:raw-path data)
              :stage       stage}))]
     (merge data {:request-context request-context}))))

(defn raw-api-gateway-v2-event
  ([] (raw-api-gateway-v2-event {}))
  ([overrides]
   (let [proxy? (get overrides :proxy? (random-boolean))
         stage (random-stage)
         api-id (random-api-id)
         host (get overrides :host
                (random-api-domain-name {:api-id api-id}))
         headers (merge {"host" host}
                   (get overrides :headers {}))
         http-method (get overrides :http-method
                       (random-http-method))

         {:keys [route-key
                 raw-path
                 raw-query-string
                 cookies
                 headers
                 query-string-parameters
                 body
                 base64-encoded?
                 stage-variables
                 path-parameters]}
         (api-gateway-v2-event-data
           (merge overrides
             {:proxy?      proxy?
              :headers     headers
              :http-method http-method}))

         request-context
         (get overrides :request-context
           (raw-api-gateway-v2-request-context
             {:proxy?      proxy?
              :api-id      api-id
              :domain-name host
              :route-key   route-key
              :path        raw-path
              :http-method http-method
              :stage       stage}))]
     {"version"               "2.0"
      "routeKey"              route-key
      "rawPath"               raw-path
      "rawQueryString"        raw-query-string
      "cookies"               cookies
      "headers"               headers
      "queryStringParameters" query-string-parameters
      "body"                  body
      "isBase64Encoded"       base64-encoded?
      "stageVariables"        stage-variables
      "pathParameters"        path-parameters
      "requestContext"        request-context})))
