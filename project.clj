(defproject misfell-todomvc "0.1.0-SNAPSHOT"
  :description "Experimental TodoMVC"
  :url "https://github.com/nilern/misfell-todomvc"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url  "https://www.eclipse.org/legal/epl-2.0/"}

  :dependencies [[org.clojure/clojure "1.10.0"]
                 [org.clojure/clojurescript "1.10.520"]]
  :plugins [[lein-cljsbuild "1.1.7"]]

  :cljsbuild {:builds [{:id           "dev"
                        :source-paths ["src"]
                        :compiler     {:output-to  "resources/public/js/main.js"
                                       :output-dir "resources/public/js"
                                       :main       misfell-todomvc.core
                                       :asset-path "js"}}]}
  :target-path "target/%s")
