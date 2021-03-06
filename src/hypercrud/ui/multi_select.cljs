(ns hypercrud.ui.multi-select
  (:require [hypercrud.client.tx :as tx]
            [hypercrud.ui.auto-control :refer [auto-control]]))


(defmulti multi-select-markup (fn [click-add! control-tuples] :default))


(defn multi-select* [markupfn add-item! field anchors props {:keys [user-with!] :as param-ctx}]
  (assert false "todo readonly and test this")
  (let [control-tuples (seq (mapv (fn [inner-value]
                                    (let [click-remove! #(user-with! (tx/edit-entity (:db/id (:entity param-ctx)) (:field/attribute field) [inner-value] nil))
                                          new-field (assoc field :cardinality :db.cardinality/one)
                                          param-ctx (assoc param-ctx :value inner-value) ; or whatever
                                          control [auto-control new-field anchors param-ctx]]
                                      [inner-value click-remove! control]))
                                  (:value param-ctx)))]
    (markupfn add-item! control-tuples)))


(defmethod multi-select-markup :default [click-add! control-tuples & [css-class]]
  [:div.value {:class css-class}
   (map (fn [[eid click-remove! control]]
          ^{:key (str eid)}                                 ;(str eid) so this works when eid is nil
          [:div.multi-select-group
           [:button {:on-click click-remove!} "-"]
           control])
        control-tuples)
   [:button {:on-click click-add!} "+"]])
