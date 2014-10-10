(ns pi.models.datomic
  (:require [datomic.api :refer [q db] :as d]
            [com.stuartsierra.component :as component]))

(defn- connect-to-database [host port]
  (let [uri (str "datomic:free://" host ":" port "/pi")]
    (d/delete-database uri)
    (d/create-database uri)
    (d/connect uri)))

(defrecord Database [host port connection]
  component/Lifecycle
  (start [component]
    (println "starting database")
    (if (not connection)
      (let [conn (connect-to-database host port)]
        (assoc component :connection conn))))

  (stop [component]
    (println "stopping database")
    (if connection
      (assoc component :connection nil))))

(defn database [host port]
  (map->Database {:host host :port port}))
