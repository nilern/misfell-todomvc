(ns misfell-todomvc.core
  (:require [mistletoe.signal :as signal :refer [smap]]
            [mistletoe.dom :refer [append-child!]]

            [misfell-todomvc.views :refer [header todos-view footer]]
            [misfell-todomvc.controller :refer [handle-event]]
            [misfell-todomvc.executor :refer [handle-effects]]))

(defn main []
  (let [domain-state (signal/source {:id-counter 0
                                     :todos      {}})
        ui-state (signal/source {:todos   (into {}
                                                (map (fn [[id _]] [id {:id      id
                                                                       :editing false}]))
                                                (:todos @domain-state))
                                 :filters [{:title    "All"
                                            :route    "#/"
                                            :selected true}
                                           {:title    "Active"
                                            :route    "#/active"
                                            :selected false}
                                           {:title    "Completed"
                                            :route    "#/completed"
                                            :selected false}]})

        emit (comp (partial handle-effects domain-state ui-state) handle-event)]

    (emit [:init])

    ;; GOTCHA: Have to use append-child! instead of .appendChild or reactivity does not manifest:
    (doto (aget (.getElementsByClassName js/document "todoapp") 0)
      (append-child! (header emit))
      (append-child! (todos-view emit (smap :todos domain-state) (smap :todos ui-state)))
      (append-child! (footer (smap :todos domain-state) (smap :filters ui-state))))))
