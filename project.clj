(defproject pi "0.1.0-SNAPSHOT"
  :description "Hyper Local Information"
  :url "http://example.com/FIXME"
  :license {:name "MIT"
            :url ""}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [ring/ring-core "1.3.1"]
                 [javax.servlet/servlet-api "2.5"] ;dev only ?
                 [http-kit "2.1.18"]
                 [compojure "1.1.9"]
                 [org.clojure/data.json "0.2.5"]
                 ]
  :main main
  :min-lein-version "2.0.0"
  )
