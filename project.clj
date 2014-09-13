(defproject pi "0.1.0-SNAPSHOT"
  :description "Hyper Local Information"
  :url "http://example.com/FIXME"
  :license {:name "MIT"
            :url ""}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/clojurescript "0.0-2322"]
                 [environ "1.0.0"]
                 [ring/ring-core "1.3.1"]
                 [javax.servlet/servlet-api "2.5"] ;dev only ?
                 [http-kit "2.1.18"]
                 [compojure "1.1.9"]
                 [org.clojure/data.json "0.2.5"]
                 [geo-clj "0.3.15"]
                 ]
  :min-lein-version "2.0.0"
  :main main
  :plugins [[lein-cljsbuild "1.0.3"]]
  :cljsbuild {:builds [{:source-paths ["src-cljs"]
                        :compiler {:output-to "static/main.js"
                                   :optimizations :whitespace
                                   :pretty-print true}}]}
  )
