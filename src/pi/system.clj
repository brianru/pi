(ns pi.system
  (:require [com.stuartsierra.component :as component]
            [pi.handlers.chsk :refer [chsk-server]]
            [pi.handlers.http :refer [http-server]]
            [pi.models.datomic :refer [database]]))

(defn system [config-options]
  (let [{:keys [port env]} config-options]
    (component/system-map
     :db          (database "localhost" 2170)
     :chsk-server (chsk-server)
     :http-server (component/using (http-server port env)
                                   {:chsk-server :chsk-server})
     )))
