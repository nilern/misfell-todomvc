(ns misfell-todomvc.core
  (:require [mistletoe.dom :refer [el]]))

(defn- header []
  (el :header :class "header"
      (el :h1 "todos")
      (el :input :class "new-todo" :placeholder "What needs to be done?" :autofocus true)))

(defn main
  "I don't do a whole lot ... yet."
  [& args]
  (let [todoapp (aget (.getElementsByClassName js/document "todoapp") 0)]
    (.prepend todoapp (header))))                           ; FIXME: prepend is experimental