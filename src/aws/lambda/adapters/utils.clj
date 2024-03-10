(ns aws.lambda.adapters.utils
  (:require
    [jason.core :as jason]
    [camel-snake-kebab.extras :as csk-extras]))

(def ->json (jason/new-json-encoder))
(def <-json (jason/new-json-decoder))

(defn make-log-event-type-fn [ns category]
  (fn [type]
    (keyword ns (str (name category) "." (name type)))))

(defn transform-keys-shallow [t coll]
  (update-keys coll t))

(defn transform-keys-deep [t coll]
  (csk-extras/transform-keys t coll))

(defn remove-keys [m ks]
  (apply dissoc m ks))

(defn split-map [m ks]
  [(remove-keys m ks)
   (select-keys m ks)])

(defn integer-or-nil [value]
  (try
    (Integer/parseInt value)
    (catch NumberFormatException _ nil)))

(defn drill->
  ([value & steps]
   (reduce
     (fn [result step] (when result (step result)))
     value steps)))
