(ns misfell-todomvc.core
  (:require [mistletoe.signal :as signal :refer [smap]]
            [mistletoe.signal.util :refer [seqsig->sigseq map-key-cached]]
            [mistletoe.dom :refer [el]]))

;;;;

(def ^:private state
  (signal/source {:todos   {0 {:id    0
                               :title "Taste JavaScript"
                               :done  true}
                            1 {:id    1
                               :title "Buy a unicorn"
                               :done  false}}
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

(defn- todo-view [todo]
  (el :li :class (smap #(if (:done %) "completed" "") todo)
      (el :div :class "view"
          (el :input :class "toggle" :type "checkbox" :checked (smap :done todo)) ; FIXME
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

(defn- todos-view [todos]
  (el :section :class "main"
      (el :input :id "toggle-all" :class "toggle-all" :type "checkbox")
      (el :label :for "toggle-all" "Mark all as complete")
      (el :ul :class "todo-list"
          (let [todos-sigsig (smap (fn [todos-val]
                                     (for [[id _] todos-val]
                                       (smap #(get % id) todos)))
                                   todos)]
            (smap (map-key-cached (fn [_ todo] (:id @todo))
                                  todo-view)
                  todos-sigsig)))))

;;;;

(defn main
  "I don't do a whole lot ... yet."
  []
  (doto (aget (.getElementsByClassName js/document "todoapp") 0)
    (.appendChild (header))
    (.appendChild (todos-view (smap :todos state)))
    (.appendChild (footer (smap :todos state) (smap :filters state)))))