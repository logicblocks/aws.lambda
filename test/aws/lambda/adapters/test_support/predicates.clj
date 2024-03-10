(ns aws.lambda.adapters.test-support.predicates)

(defn kebab-case-keyword? [value]
  (and (keyword? value)
    (re-matches #"^[a-z0-9\?\-]*$" (name value))))

(defn kebab-case-string? [value]
  (and (string? value)
    (re-matches #"^[a-z0-9\?\-]+$" value)))

(defn camel-case-string? [value]
  (and (string? value)
    (re-matches #"[a-z]+((\d)|([A-Z0-9][a-z0-9]+))*([A-Z])?" value)))

