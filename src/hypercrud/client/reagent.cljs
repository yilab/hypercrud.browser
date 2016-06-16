(ns hypercrud.client.reagent
  (:require [hypercrud.client.core :as hypercrud]
            [promesa.core :as p]
            [reagent.core :as reagent]))


(defn- loader [f comp & [loading-comp]]
  (let [cmp (reagent/current-component)
        v' (f cmp [loader f comp loading-comp])]
    (cond
      (p/resolved? v') (comp (p/extract v'))
      (p/rejected? v') [:pre (p/extract v')]
      :pending? (if loading-comp (loading-comp) [:div "loading"]))))


(defn entity [client eid comp & [loading-comp]]
  (loader (partial hypercrud/entity* client eid) comp loading-comp))


(defn query
  ([client q comp]
   (query client q [] comp))
  ([client q query-params comp & [loading-comp]]
   (loader (partial hypercrud/query* client q query-params) comp loading-comp)))


(defn enter [client comp & [loading-comp]]
  (loader (partial hypercrud/enter* client) comp loading-comp))