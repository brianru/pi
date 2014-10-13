(ns pi.communications.gateway
  (:require [com.stuartsierra.component :as component]
            [crypto.password.scrypt :as pw]
            [datomic.api :as d]))

(defn register! [{:keys [conn]} username password]
  (let [hash-pass (pw/encrypt password)
        user      (d/q '[:find ?e
                         :in $ ?username
                         :where [?e :user/name ?username]]
                       (d/db conn)
                       username)]
    (if (empty? user)
      @(d/transact conn
                   [{:db/id (d/tempid :db.part/user)
                     :user/id (d/squuid)
                     :user/name username
                     :user/password hash-pass}])
      false)))

(defn login! [{:keys [conn] :as db} username password]
  (let [user (d/entity (d/db conn)
                       (ffirst
                        (d/q '[:find ?e
                               :in $ ?username
                               :where [?e :user/name ?username]]
                             (d/db conn)
                             username)))]
    (if-let [cur-pass (:user/password user)]
      (pw/check password cur-pass)
      false)))

(defn logout! [db username]
  username)

(defrecord Gateway [db]
  component/Lifecycle
  (start [this]
    (assoc this :fns {:register (partial register! db)   
                      :login    (partial login!    db)
                      :logout   (partial logout!   db)}))
  
  (stop [this]
    (assoc this :fns nil)))

(defn gateway []
  (Gateway. nil))
