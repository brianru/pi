(ns pi.system
  (:require [com.stuartsierra.component :as component]
            [pi.server.chsk :refer [chsk-server]]
            [pi.server.http :refer [http-server]]
            [pi.comm.private :refer [private]]
            [pi.comm.gateway :refer [gateway]]
            [pi.data.datomic :refer [database]]))

(defn system
  "Design guidelines:
   - inject dependencies into components where the data originates
   - TODO when to build sub-systems?
   "
  [config-options]
  (let [{:keys [port env]} config-options]
    (component/system-map
     ;; data layer
     :db (database "localhost" 4334)
     ;; TODO core ns component as db interface layer?

     ;; logic layer / core / business logic
     :core-in  (component/using (in)  {:comms :private-comms})
     :core-out (component/using (out) {:comms :private-comms})

     ;; comms layer
     :private-comms (component/using (private)
                                     {:db :db
                                      :chsk-server :chsk-server})
     :gateway-comms (component/using (gateway) {:db :db})

     ;; server layer
     :chsk-server (component/using (chsk-server)
                                   {:comms :private-comms})
     :http-server (component/using (http-server port env)
                                   {:comms :gateway-comms
                                    :chsk-server :chsk-server}))))
