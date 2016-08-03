(ns hypercrud.client.tx)


(defn tempid? [eid] (< eid 0))


(defn simplify [simplified-tx next-stmt]
  (let [[op e a v] next-stmt
        g (group-by (fn [[op' e' a' v']] (and (= e' e) (= a' a) (= v' v)))
                    simplified-tx)
        [op' e' a' v'] (first (get g true))                 ;if this count > 1, we have duplicate stmts, they are harmless and discard dups here.
        non-related (get g false)]
    (cond
      (= op :db/add) (if (= op' :db/retract)
                       non-related                          ;we have a related previous stmt that cancels us and it out
                       (conj non-related next-stmt))
      (= op :db/retract) (if (= op' :db/add)
                           non-related                      ;we have a related previous stmt that cancels us and it out
                           (conj non-related next-stmt))
      :else (throw "match error"))))


(defn into-tx [tx more-statements]
  "We don't care about the cardinality (schema) because the UI code is always
  retracting values before adding new value, even in cardinality one case. This is a very
  convenient feature and makes the local datoms cancel out properly always to not cause
  us to re-assert datoms needlessly in datomic"
  (reduce simplify tx more-statements))


(defn apply-statements-to-entity [schema statements {id :db/id :as entity}]
  (->> statements
       (filter (fn [[op e a v]] (= id e)))
       (reduce (fn [acc [op _ a v]]
                 (let [cardinality (get-in schema [a :db/cardinality])
                       _ (assert cardinality (str "schema attribute not found: " (pr-str a)))]
                   (cond
                     (and (= op :db/add) (= cardinality :db.cardinality/one)) (assoc acc a v)
                     (and (= op :db/retract) (= cardinality :db.cardinality/one)) (dissoc acc a)
                     (and (= op :db/add) (= cardinality :db.cardinality/many)) (update-in acc [a] (fnil #(conj % v) #{}))
                     (and (= op :db/retract) (= cardinality :db.cardinality/many)) (update-in acc [a] (fnil #(disj % v) #{}))
                     :else (throw "match error"))))
               entity)))


(defn entity->statements [schema {eid :db/id :as entity}]   ; entity always has :db/id
  (->> (dissoc entity :db/id)
       (mapcat (fn [[attr val]]
                 (let [cardinality (get-in schema [attr :db/cardinality])
                       valueType (get-in schema [attr :db/valueType])
                       _ (assert cardinality (str "schema attribute not found: " (pr-str attr)))]
                   (if (= valueType :db.type/ref)
                     (cond
                       (= cardinality :db.cardinality/one) [[:db/add eid attr (:db/id val)]]
                       (= cardinality :db.cardinality/many) (mapv (fn [val] [:db/add eid attr (:db/id val)]) val))
                     (cond
                       (= cardinality :db.cardinality/one) [[:db/add eid attr val]]
                       (= cardinality :db.cardinality/many) (mapv (fn [val] [:db/add eid attr val]) val))))))))


(defn entity? [v]
  (map? v))

(defn entity-children [schema entity]
  (mapcat (fn [[attr val]]
            (let [cardinality (get-in schema [attr :db/cardinality])
                  valueType (get-in schema [attr :db/valueType])]
              (cond
                (and (= valueType :db.type/ref) (= cardinality :db.cardinality/one)) [val]
                (and (= valueType :db.type/ref) (= cardinality :db.cardinality/many)) (vec val)
                :else [])))
          entity))


(defn pulled-tree-to-statements [schema pulled-tree]
  ;; branch nodes are type entity. which right now is hashmap.
  (let [traversal (tree-seq (fn [v] (entity? v))
                            #(entity-children schema %)
                            pulled-tree)]
    (mapcat #(entity->statements schema %) traversal)))