(ns hypercrud.util.base-64-url-safe
  (:require [clojure.string :as string]
            [hypercrud.platform.base-64 :as base-64]))


(defn encode [s]
  (-> s
      (base-64/encode-string)
      (string/replace "+" "-")
      (string/replace "/" "_")
      (string/replace "=" ",")))

(defn decode [s]
  (-> s
      (string/replace "-" "+")
      (string/replace "_" "/")
      (string/replace "," "=")
      (base-64/decode-string)))
