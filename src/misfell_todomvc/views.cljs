(ns misfell-todomvc.views
  (:require [clojure.string :as str]
            [mistletoe.dom :refer [el]]
            [mistletoe.signal :refer [smap]]
            [mistletoe.signal.util :refer [seqsig->sigseq map-key-cached]]))

(defn header [emit]
  (el :header :class "header"
      (el :h1 "todos")
      (el :input :type "text" :class "new-todo"
          :placeholder "What needs to be done?" :autofocus true
          :onkeydown (fn [ev]
                       (when (= (.-key ev) "Enter")
                         (let [title (.. ev -target -value trim)]
                           (when-not (str/blank? title)
                             (emit [:new-todo title]))))))))

(defn- todo-view-class [{:keys [done editing]}]
  (str (when done "completed ") (when editing "editing")))

(defn- todo-view [emit todo]
  (el :li :class (smap todo-view-class todo)
      (el :div :class "view"
          (el :input :class "toggle" :type "checkbox"
              :checked (smap #(or (:done %) nil) todo)
              :onchange (fn [_] (emit [:toggle-done (:id @todo)])))
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

(defn footer [todos filters]
  (el :footer :class "footer"
      :style {:display (smap #(if (empty? %) "none" "block") todos)}
      (todos-count todos)
      (filters-view filters)
      (el :button :class "clear-completed" "Clear completed")))

(defn todos-view [emit todos ui-todos]
  (el :section :class "main"
      :style {:display (smap #(if (empty? %) "none" "block") todos)}
      (el :input :type "checkbox" :id "toggle-all" :class "toggle-all"
          :checked (smap (fn [todos] (when (every? (fn [[_ {:keys [done]}]] done) todos) "")) todos)
          :onclick (fn [ev] (emit [:toggle-all (.. ev -target -checked)])))
      (el :label :for "toggle-all" "Mark all as complete")
      (el :ul :class "todo-list"
          (let [todos-sigsig (smap (fn [todos-val]
                                     (for [[id _] todos-val]
                                       (smap (fn [todos ui-todos] (merge (get todos id) (get ui-todos id)))
                                             todos ui-todos)))
                                   todos)]
            (smap (map-key-cached (fn [_ todo] (:id @todo))
                                  (partial todo-view emit))
                  todos-sigsig)))))


