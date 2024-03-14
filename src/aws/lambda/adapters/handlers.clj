(ns aws.lambda.adapters.handlers
  (:require
   [cartus.core :as log]
   [cartus.null :as cn]

   [tick.core :as tc]

   [aws.lambda.adapters.utils :as utils]
   [aws.lambda.adapters.context :as context])
  (:import
   [java.io InputStream
    OutputStream]
   [com.amazonaws.services.lambda.runtime Context
    RequestStreamHandler]))

(defn handle-request
  [^RequestStreamHandler handler
   ^InputStream in
   ^OutputStream out
   ^Context context]
  (.handleRequest handler in out context))

(defmacro def-lambda-handler
  [{:keys [name
           initialiser
           request-handler
           clock]}]
  (let [initialiser (or initialiser (fn [] nil))
        request-handler (or request-handler (fn [_ _ _ _]))
        prefix (gensym)
        init-method (symbol (str prefix "init"))
        handle-request-method (symbol (str prefix "handleRequest"))
        namespace (str (ns-name *ns*))]
    `(do
       (gen-class
         :name ~name
         :state "state"
         :init "init"
         :prefix ~prefix
         :implements
         [com.amazonaws.services.lambda.runtime.RequestStreamHandler])

       (defn ~init-method []
         [[] (~initialiser)])

       (defn ~handle-request-method
         [this#
          ^InputStream in#
          ^OutputStream out#
          ^Context context#]
         (let [state# (.state this#)
               clock# (or (:clock state#) ~clock (tc/clock))
               logger# (or (:logger state#) (cn/logger))
               log-event-type#
               (utils/make-log-event-type-fn ~namespace :request-handler)
               context# (context/normalise-context context# clock#)]
           (log/debug logger#
             (log-event-type# :handling)
             {:lambda {:context context#}})
           (~request-handler state# in# out# context#))))))
