(ns pi.data.datomic
  (:require [datomic.api :refer [q db] :as d]
            [com.stuartsierra.component :as component]))

(defn- connect-to-database [host port]
  (let [uri (str "datomic:free://" host ":" port "/pi")]
    (d/delete-database uri)
    (d/create-database uri)
    (let [conn (d/connect uri)]
      @(d/transact conn (read-string (slurp "src/pi/data/schema.edn")))
      conn)))

(defrecord Database [host port conn]
  component/Lifecycle
  (start [component]
    (println "starting database")
    (if (not conn)
      (let [conn (connect-to-database host port)]
        (assoc component :conn conn))))

  (stop [component]
    (println "stopping database")
    (if conn
      (assoc component :conn nil))))

(defn database [host port]
  (map->Database {:host host :port port}))
