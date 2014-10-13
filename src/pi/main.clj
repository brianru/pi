(ns pi.main
  (:gen-class)
  (:require [com.stuartsierra.component :as component]
            [pi.system :refer [system]]
            [environ.core :refer [env]]
            ))

(defn -main [& args]
  (let [port (Integer. (or (env :port) "9899"))
        env  (if (env :port) "prod" "test")]
    (component/start-system
     (system {:port port
              :env  env}))))
