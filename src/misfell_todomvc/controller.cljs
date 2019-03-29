(ns misfell-todomvc.controller
  (:require [cats.core :refer [mlet return]]
            [fell.core :refer [request-eff]]
            
            [misfell-todomvc.models.todos :as todos
             :refer [serialize-domain-state deserialize-domain-state]]
            [misfell-todomvc.local-storage :refer [storage-key]]))

(defn- load-domain-state []
  (mlet [state-str (request-eff [:local-storage/get storage-key])]
    (if state-str
      (request-eff [:domain-state :set (deserialize-domain-state state-str)])
      (return nil))))

(defn- update-domain-state [f & args]
  (mlet [domain-state (request-eff [:domain-state :get])
         :let [domain-state (apply f domain-state args)]
         _ (request-eff [:local-storage/set storage-key (serialize-domain-state domain-state)])]
    (request-eff [:domain-state :set domain-state])))

(defn handle-event [[tag & args]]
  (case tag
    :init (load-domain-state)
    :new-todo (let [[title] args]
                (update-domain-state todos/create title))
    :toggle-done (let [[id] args]
                   (update-domain-state todos/toggle-done id))
    :toggle-all (let [[done] args]
                  (update-domain-state todos/toggle-all done))))


