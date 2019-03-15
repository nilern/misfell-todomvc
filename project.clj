(defproject misfell-todomvc "0.1.0-SNAPSHOT"
  :description "Experimental TodoMVC"
  :url "https://github.com/nilern/misfell-todomvc"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url  "https://www.eclipse.org/legal/epl-2.0/"}

  :repositories [["jitpack" "https://jitpack.io"]]          ; HACK to avoid releasing half-baked deps
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [org.clojure/clojurescript "1.10.520"]
                 [funcool/promesa "1.9.0"]
                 [funcool/cats "2.3.2"]
                 [com.github.nilern/fell "master-SNAPSHOT"]
                 [com.github.nilern/mistletoe "master-SNAPSHOT"]]
  :plugins [[lein-cljsbuild "1.1.7"]
            [lein-npm "0.6.2"]]

  :cljsbuild {:builds [{:id           "dev"
                        :source-paths ["src"]
                        :compiler     {:output-to  "resources/public/js/main.js"
                                       :output-dir "resources/public/js"
                                       :main       misfell-todomvc.core
                                       :asset-path "js"}}]}
  :npm {:dependencies [[todomvc-app-css "^2.0.0"]
                       [todomvc-common "^1.0.0"]]}

  :target-path "target/%s")
