(ns hypercrud.ui.auto-control-default
  (:require [hypercrud.ui.auto-control :as auto-control]
            [hypercrud.ui.code-editor :as code-editor]
            [hypercrud.ui.widget :as widget]))


(defn code-iframe [fieldinfo graph metatype forms value expanded-cur change! transact! tempid!]
  [:div.code-iframe
   [code-editor/code-editor* value change!]
   [:iframe {:src "http://www.hypercrud.com/projects/17592186045422/" #_(str "projects/" eid)}]])


(defn widget-for-fieldinfo [{:keys [datatype cardinality component name]}]
  (cond
    (= name :project/code) code-iframe

    (and (= datatype :string) (= cardinality :one)) widget/input
    (and (= datatype :code) (= cardinality :one)) widget/code-editor
    (and (= datatype :ref) (= cardinality :one) component) widget/select-ref-component
    (and (= datatype :ref) (= cardinality :many) component) widget/multi-select-ref-component
    (and (= datatype :ref) (= cardinality :one)) widget/select-ref
    (and (= datatype :ref) (= cardinality :many)) widget/multi-select-ref
    :else widget/default))


(defmethod auto-control/auto-control :default
  [fieldinfo graph metatype forms value expanded-cur change! transact! tempid!]
  [(widget-for-fieldinfo fieldinfo) fieldinfo graph metatype forms value expanded-cur change! transact! tempid!])
