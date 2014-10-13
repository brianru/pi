(ns pi.system
  (:require [com.stuartsierra.component :as component]
            [pi.server.chsk :refer [chsk-server]]
            [pi.server.http :refer [http-server]]
            [pi.communications.private :refer [verbs]]
            [pi.communications.gateway :refer [gateway]]
            [pi.data.datomic :refer [database]]))

(defn system [config-options]
  (let [{:keys [port env]} config-options]
    (component/system-map
     ;; data layer
     :db          (database "localhost" 4334)

     ;; comms layer
     :private-comms (verbs)
     :gateway-comms (gateway)

     ;; server layer
     ;; TODO should this be a sub-system instead of 2 components?
     :chsk-server (chsk-server)
     :http-server (component/using (http-server port env)
                                   {:chsk-server :chsk-server}))))
