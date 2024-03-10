(disable-warning
  {:linter :constant-test
   :for-macro 'clojure.core/or
   :if-inside-macroexpansion-of #{'aws.lambda.adapters.handlers/def-lambda-handler}
   :within-depth 10})
