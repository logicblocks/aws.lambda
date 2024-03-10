(ns aws.lambda.adapters.context
  (:require
    [aws.lambda.adapters.clock :as clock])
  (:import
    [com.amazonaws.services.lambda.runtime
     Client
     ClientContext
     CognitoIdentity
     Context]))

(defn normalise-identity [^CognitoIdentity identity]
  (when identity
    {:identity-id      (.getIdentityId identity)
     :identity-pool-id (.getIdentityPoolId identity)}))

(defn normalise-client [^Client client]
  (when client
    {:installation-id  (.getInstallationId client)
     :app-title        (.getAppTitle client)
     :app-version-name (.getAppVersionName client)
     :app-version-code (.getAppVersionCode client)
     :app-package-name (.getAppPackageName client)}))

(defn normalise-client-context [^ClientContext client-context]
  (when client-context
    (let [client (.getClient client-context)]
      {:custom      (.getCustom client-context)
       :environment (.getEnvironment client-context)
       :client      (normalise-client client)})))

(defn normalise-context
  ([^Context context]
   (normalise-context context (clock/system-clock)))
  ([^Context context clock]
   (let [aws-request-id (.getAwsRequestId context)

         log-group-name (.getLogGroupName context)
         log-stream-name (.getLogStreamName context)

         function-name (.getFunctionName context)
         function-version (.getFunctionVersion context)

         invoked-function-arn (.getInvokedFunctionArn context)

         ; unfortunately, the context interface doesn't expose the underlying
         ; deadline time, so if we want context to be purely data, we have to
         ; calculate something less than or equal to the real deadline time
         now-in-millis (clock/millis clock)
         remaining-time-in-millis (.getRemainingTimeInMillis context)
         deadline-time-in-millis (+ now-in-millis remaining-time-in-millis)

         memory-limit-in-mb (.getMemoryLimitInMB context)

         identity (.getIdentity context)
         client-context (.getClientContext context)]
     {:aws-request-id          aws-request-id
      :log-group-name          log-group-name
      :log-stream-name         log-stream-name

      :function-name           function-name
      :function-version        function-version

      :invoked-function-arn    invoked-function-arn

      :identity                (normalise-identity identity)
      :client-context          (normalise-client-context client-context)

      :deadline-time-in-millis deadline-time-in-millis
      :memory-limit-in-mb      memory-limit-in-mb})))
