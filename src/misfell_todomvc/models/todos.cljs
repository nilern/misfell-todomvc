(ns misfell-todomvc.models.todos)

(defn create [{id :id-counter :as domain-state} title]
  (-> domain-state
      (update :todos assoc id {:id id, :title title, :done false})
      (update :id-counter inc)))

(defn toggle-done [domain-state id]
  (update-in domain-state [:todos id :done] not))

(defn toggle-all [{:keys [todos] :as domain-state} done]
  (assoc domain-state :todos (into {}
                                   (map (fn [[id todo]] [id (assoc todo :done done)]))
                                   todos)))

(defn serialize-domain-state [domain-state]
  (.stringify js/JSON (clj->js domain-state)))

(defn deserialize-domain-state [state-str]
  (-> (.parse js/JSON state-str)
      (js->clj :keywordize-keys true)
      (update :todos (fn [todos] (into {} (map (fn [[id todo]] [(.parseInt js/Number (name id)) todo])) todos)))))

