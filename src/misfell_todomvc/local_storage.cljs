(ns misfell-todomvc.local-storage
  (:require [cats.core :refer [return bind]]
            [cats.labs.promise :as promise]
            [promesa.core :refer [promise]]
            [fell.core :refer [handle-relay]]
            [fell.lift :refer [lift]]))

(def storage-key "todos-misfell")

;;; FIXME: JS Promises are too eager to side effect and not really monads.

(defn- get-item [k]
  (promise (fn [resolve _] (resolve (.. js/window -localStorage (getItem k))))))

(defn- set-item [k v]
  (promise (fn [resolve _] (resolve (.. js/window -localStorage (setItem k v))))))

(defn run-local-storage [freer]
  (handle-relay #(= (namespace (first %)) "local-storage")
                (comp lift (partial return promise/context))
                (fn [[tag & args] cont]
                  (case tag
                    :local-storage/get (let [[k] args]
                                         (bind (lift (get-item k)) cont))
                    :local-storage/set (let [[k v] args]
                                         (bind (lift (set-item k v)) cont))))
                freer))

