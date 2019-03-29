(ns misfell-todomvc.executor
  (:require [cats.core :refer [mlet]]
            [cats.labs.promise :as promise]
            [fell.lift :refer [run-lift]]
            [fell.state :refer [state-runner]]

            [misfell-todomvc.local-storage :refer [run-local-storage]]))

(def ^:private run-domain-state (state-runner :domain-state))

(def ^:private run-ui-state (state-runner :ui-state))

(defn handle-effects [domain-state ui-state freer]
  (mlet [[[_ domain-state*] ui-state*] (->> (-> freer
                                                (run-domain-state @domain-state)
                                                (run-ui-state @ui-state)
                                                run-local-storage)
                                            (run-lift promise/context))]
    ;; And eventually we (in a Promise here!) essentially `unsafePerformIO`:
    (reset! domain-state domain-state*)
    (reset! ui-state ui-state*)))


