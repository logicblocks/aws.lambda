(ns aws.lambda.adapters.api-gateway.handlers
  (:require
   [clojure.java.io :as io]

   [cartus.core :as log]
   [cartus.null :as cn]

   [aws.lambda.adapters.utils :as utils]
   [aws.lambda.adapters.handlers :refer [def-lambda-handler]]
   [aws.lambda.adapters.api-gateway.transformers :as transformers]
   [aws.lambda.adapters.api-gateway.responses :as responses]
   [aws.lambda.adapters.api-gateway.events :as events]))

(defmacro def-api-gateway-handler
  [{:keys [name
           initialiser
           request-handler
           exception-handler
           clock]}]
  (let [request-handler (or request-handler (fn [_ _ _]))
        exception-handler
        (or exception-handler
          (fn [ex]
            {:status-code     500
             :headers         {"content-type" "application/json"}
             :base64-encoded? false
             :body
             (utils/->json {"message" (ex-message ex)})}))
        namespace (str (ns-name *ns*))]
    `(def-lambda-handler
       {:name        ~name
        :clock       ~clock
        :initialiser ~initialiser
        :request-handler
        (fn [state# in# out# context#]
          (let [logger# (get state# :logger (cn/logger))
                log-event-type#
                (utils/make-log-event-type-fn ~namespace :request-handler)]
            (try
              (let [event# (events/normalise-event
                             (utils/<-json
                               (io/reader in#)))]
                (log/debug logger#
                  (log-event-type# :read-event)
                  {:lambda {:event   event#
                            :context context#}})
                (let [response# (~request-handler state# event# context#)]
                  (log/debug logger#
                    (log-event-type# :writing-response)
                    {:lambda {:response response#
                              :context  context#}})
                  (spit out#
                    (utils/->json
                      (responses/normalise-response response#)))))
              (catch Exception e#
                (log/error logger#
                  (log-event-type# :unhandled-exception)
                  {:lambda {:context context#}}
                  {:exception e#})
                (spit out#
                  (utils/->json
                    (responses/normalise-response
                      (~exception-handler e#))))))))})))

(defmacro def-api-gateway-ring-handler
  [{:keys [name
           initialiser
           defaults
           ring-handler
           exception-handler
           clock]}]
  (let [namespace (str (ns-name *ns*))
        request-transformer-options {:defaults (:request defaults)}]
    `(def-api-gateway-handler
       {:name              ~name
        :clock             ~clock
        :initialiser       ~initialiser
        :exception-handler ~exception-handler
        :request-handler
        (fn [state# event# context#]
          (let [logger# (get state# :logger (cn/logger))
                log-event-type#
                (utils/make-log-event-type-fn ~namespace :request-handler)
                ring-handler# (or (:ring-handler state#) ~ring-handler)
                request# (transformers/api-gateway-request->ring-request
                           event# context# ~request-transformer-options)

                _# (log/debug logger#
                     (log-event-type# :handling-ring-request)
                     {:ring   {:request request#}
                      :lambda {:context context#}})

                response# (ring-handler# request#)

                _# (log/debug logger#
                     (log-event-type# :received-ring-response)
                     {:ring   {:request  request#
                               :response response#}
                      :lambda {:context context#}})

                response# (transformers/ring-response->api-gateway-response
                            response#)]
            response#))})))
