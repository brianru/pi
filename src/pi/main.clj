(ns pi.main
  (:gen-class)
  (:require [pi.handlers.http :as http]
            [pi.handlers.chsk :as chsk]))

(defn start! []
  (chsk/start-router!)
  (http/start-server!))

(defn -main [& args]
  (start!))
