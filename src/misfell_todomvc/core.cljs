(ns misfell-todomvc.core
  (:require [mistletoe.signal :as signal :refer [smap]]
            [mistletoe.signal.util :refer [seqsig->sigseq map-key-cached]]
            [mistletoe.dom :refer [el append-child!]]))

;;;;

(def ^:private domain-state
  (signal/source {:todos {0 {:id    0
                             :title "Taste JavaScript"
                             :done  true}
                          1 {:id    1
                             :title "Buy a unicorn"
                             :done  false}}}))

(def ^:private ui-state
  (signal/source {:todos   (into {}
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
                             :selected false}]}))

;;;;

(defn- header []
  (el :header :class "header"
      (el :h1 "todos")
      (el :input :class "new-todo" :placeholder "What needs to be done?" :autofocus true)))

(defn- todo-view-class [{:keys [done editing]}]
  (str (when done "completed ") (when editing "editing")))

(defn- todo-view [todo]
  (el :li :class (smap todo-view-class todo)
      (el :div :class "view"
          (el :input :class "toggle" :type "checkbox"
              :checked (smap #(or (:done %) nil) todo)
              :onchange #(swap! domain-state update-in [:todos (:id @todo) :done] not)) ; HACK
          (el :label (smap :title todo))
          (el :button :class "destroy"))

      (el :input :class "edit" :value (smap :title todo))))

(defn- todos-count [todos]
  (el :span :class "todo-count"
      (el :strong (smap (fn [todos] (count (filter (fn [[_ {:keys [done]}]] (not done)) todos)))
                        todos))
      " item left"))

(defn- filters-view [filters]
  (el :ul :class "filters"
      (for [filter-sig (seqsig->sigseq filters)]
        (el :li
            (el :a :class (smap #(if (:selected %) "selected" "") filter-sig)
                :href (smap :route filter-sig)
                (smap :title filter-sig))))))

(defn- footer [todos filters]
  (el :footer :class "footer"
      (todos-count todos)
      (filters-view filters)
      (el :button :class "clear-completed" "Clear completed")))

(defn- todos-view [todos ui-todos]
  (el :section :class "main"
      (el :input :id "toggle-all" :class "toggle-all" :type "checkbox")
      (el :label :for "toggle-all" "Mark all as complete")
      (el :ul :class "todo-list"
          (let [todos-sigsig (smap (fn [todos-val]
                                     (for [[id _] todos-val]
                                       (smap (fn [todos ui-todos] (merge (get todos id) (get ui-todos id)))
                                             todos ui-todos)))
                                   todos)]
            (smap (map-key-cached (fn [_ todo] (:id @todo))
                                  todo-view)
                  todos-sigsig)))))

;;;;

(defn main
  "I don't do a whole lot ... yet."
  []
  ;; GOTCHA: Have to use append-child! instead of .appendChild or reactivity does not manifest:
  (doto (aget (.getElementsByClassName js/document "todoapp") 0)
    (append-child! (header))
    (append-child! (todos-view (smap :todos domain-state) (smap :todos ui-state)))
    (append-child! (footer (smap :todos domain-state) (smap :filters ui-state)))))