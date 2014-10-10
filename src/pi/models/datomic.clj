(ns pi.models.datomic
  (:require [me.raynes.conch.low-level :as sh]
            [datomic.api :refer [q db] :as d]
            [com.stuartsierra.component :as component]))

(def transactor-properties
  "resources/free-transactor-template.properties")

(defn- start-transactor [config]
  (let [p (sh/proc "/usr/local/bin/datomic-transactor" config)]
    (while true
      (try
        (sh/stream-to-out p :out)
        (catch Exception e (println (str e)))))))

(defn- connect-to-database [host port]
  (let [uri (str "datomic:free://" host ":" port "/" (d/squuid))]
    (start-transactor transactor-properties)
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
;    (.close connection)
    (if connection
      (assoc component :connection nil))))

(defn database [host port]
  (map->Database {:host host :port port}))
