(defproject pi "0.1.0-SNAPSHOT"
  :description "Hyper Local Information"
  :url "http://example.com/FIXME"
  :license {:name "MIT"
            :url ""}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/clojurescript "0.0-2341"]
                 [org.clojure/core.cache "0.6.4"]
                 [org.clojure/core.memoize "0.5.6"]
                 [org.clojure/core.async "0.1.338.0-5c5012-alpha"]
                 [environ "1.0.0"]
                 [com.taoensso/encore "1.9.1"]
                 [com.taoensso/tower "3.0.1"] ;; TODO
                 [crypto-password "0.1.3"]

                 ;; Web communications
                 [ring/ring-core "1.3.1"]
                 [ring/ring-defaults "0.1.1"]
                 [javax.servlet/servlet-api "2.5"] ;dev only ?
                 [http-kit "2.1.18"]
                 [com.taoensso/sente "1.1.0"]
                 [compojure "1.1.9"]

                 ;; Database
                 [org.clojure/java.jdbc "0.3.5"]
                 [postgresql/postgresql "8.4-702.jdbc4"]

                 ;; Client application
                 [om "0.7.3"]
                 [secretary "1.2.1"]
                 [sablono "0.2.22"] ;; TODO
                 [geo-clj "0.3.15"]
                 [com.andrewmcveigh/cljs-time "0.1.6"]
                 ]
  :min-lein-version "2.3.3"
  :main pi.main
  :plugins [[lein-cljsbuild "1.0.3"]
            [lein-marginalia "0.8.0"]]
  :java-agents [[com.newrelic.agent.java/newrelic-agent "2.19.0"]]
  :profiles {:dev {}
             :uberjar {:aot :all
                       :jar-name "pi.jar"
                       :uberjar-name "uberpi.jar"
                       :hooks [leiningen.cljsbuild]
                       }}
  :cljsbuild {:builds
              {:dev {:source-paths ["src-cljs/pi/"]
                     :compiler {:output-to "resources/public/js/main.js"
                                :output-dir "resources/public/js/out"
                                :optimizations :none
                                :pretty-print true
                                :foreign-libs [{:file "resources/public/js/externs/moment.min.js"
                                                :provides ["pi.moment"]}]
                                :source-map true}}
               :uberjar {
                         :source-paths ["src-cljs/pi/"]
                         :jar true
                         :compiler
                         {:output-to "resources/public/js/main.js"
                          :optimizations :advanced
                          :elide-asserts true
                          :pretty-print false
                          :output-wrapper false
                          :preamble ["react/react.min.js"]
                          :externs ["react/externs/react.js"
                                    "resources/public/js/externs/moment.min.js"]
                          :foreign-libs [{:file "http://momentjs.com/downloads/moment.min.js"
                                          :provides ["pi.moment"]}]
                          :closure-warnings {:externs-validation :off
                                             :non-standard-jsdoc :off}}}}}
              )
