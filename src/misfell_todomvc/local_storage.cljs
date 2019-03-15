(ns misfell-todomvc.local-storage
  (:require [cats.core :refer [return]]
            [cats.labs.promise :as promise]
            [fell.core :refer [lift handle-relay]]))

(def storage-key "todos-misfell")

;;; FIXME: JS Promises are too eager to side effect and not really monads.

(defn run-local-storage [freer]
  (handle-relay #(= (namespace (first %)) "local-storage")
                (comp lift (partial return promise/context))
                (fn [[tag & args] cont]
                  (case tag
                    :local-storage/get (let [[k] args]
                                         (cont (.. js/window -localStorage (getItem k))))
                    :local-storage/set (let [[k v] args]
                                         (cont (.. js/window -localStorage (setItem k v))))))
                freer))
