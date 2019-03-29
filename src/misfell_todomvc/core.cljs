(ns misfell-todomvc.core
  (:require [cats.core :refer [mlet return]]
            [cats.labs.promise :as promise]
            [clojure.string :as str]
            [fell.core :refer [request-eff]]
            [fell.state :refer [state-runner]]
            [fell.lift :refer [run-lift]]
            [mistletoe.signal :as signal :refer [smap]]
            [mistletoe.signal.util :refer [seqsig->sigseq map-key-cached]]
            [mistletoe.dom :refer [el append-child!]]

            [misfell-todomvc.local-storage :refer [storage-key run-local-storage]]))

;;;; # Event Handling

(defn- serialize-domain-state [domain-state]
  (.stringify js/JSON (clj->js domain-state)))

(defn- deserialize-domain-state [state-str]
  (-> (.parse js/JSON state-str)
      (js->clj :keywordize-keys true)
      (update :todos (fn [todos] (into {} (map (fn [[id todo]] [(.parseInt js/Number (name id)) todo])) todos)))))

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

(defn- handle-event [[tag & args]]
  (case tag
    :init (load-domain-state)
    :new-todo (let [[title] args]
                (update-domain-state (fn [{id :id-counter :as domain-state}]
                                       (-> domain-state
                                           (update :todos assoc id {:id id, :title title, :done false})
                                           (update :id-counter inc)))))
    :toggle-done (let [[id] args]
                   (update-domain-state update-in [:todos id :done] not))
    :toggle-all (let [[done] args]
                  (update-domain-state (fn [{:keys [todos] :as domain-state}]
                                         (assoc domain-state :todos (into {}
                                                                          (map (fn [[id todo]]
                                                                                 [id (assoc todo :done done)]))
                                                                          todos)))))))

;;;; # Effect Handling

(def ^:private run-domain-state (state-runner :domain-state))

(def ^:private run-ui-state (state-runner :ui-state))

(defn- handle-effects [domain-state ui-state freer]
  (mlet [[[_ domain-state*] ui-state*] (->> (-> freer
                                                (run-domain-state @domain-state)
                                                (run-ui-state @ui-state)
                                                run-local-storage)
                                            (run-lift promise/context))]
    ;; And eventually we (in a Promise here!) essentially `unsafePerformIO`:
    (reset! domain-state domain-state*)
    (reset! ui-state ui-state*)))

;;;; # UI

(defn- header [emit]
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

(defn- footer [todos filters]
  (el :footer :class "footer"
      :style {:display (smap #(if (empty? %) "none" "block") todos)}
      (todos-count todos)
      (filters-view filters)
      (el :button :class "clear-completed" "Clear completed")))

(defn- todos-view [emit todos ui-todos]
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

;;;; # Initialization

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
