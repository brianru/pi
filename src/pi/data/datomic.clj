(ns pi.data.datomic
  (:require [datomic.api :as d]
            [crypto.password.scrypt :as pw]
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

(comment ;; demo

 (def conn (connect-to-database "localhost" 4334))

 (def b (d/db conn))

 ;; find all user-defined db functions
 (d/q '[:find ?ident ?code
        :where
        [?e :db/ident ?ident]
        [?e :db/fn ?code]
        ]
      b)
 
 (d/q '[:find ?ident ?code
        :where
        [?e :db/ident ?ident]
        [?e :db/valueType :db.type/fn
        [?e :db/value ?code]]
        ]
      b)

 (let [ent (d/entity b :post/submit)
       _   (d/touch ent)] ;; b/c d/entity is lazy
   ent)

 (def postSubmit (:db/fn (d/entity b :post/submit)))

 (postSubmit b {:db/id 3 :user/location 42} "apple butt")

 (def userRegister (:db/fn (d/entity b :user/register)))
 (userRegister b "brian" "rubinton")
 
 
 )
