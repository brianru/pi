(ns pi.communications.gateway
  (:require [com.stuartsierra.component :as component]
            [crypto.password.scrypt :as pw]))

(defn register! [db username password]
  (let [hash-pass (pw/encrypt password)
        user      nil] ;;try to get user from db
    (if-not user
      nil ;; add user to db
      false)))
;        (ref-set all-users
;                 (assoc @all-users user-id
;                        (->User user-id
;                                hash-pass
;                                {:latitude nil :longitude nil}
;                                nil)))

(defn login! [{:keys [conn] :as db} username password]
  (let [user nil] ;; todo get user from db
    (if-let [cur-pass (:password user)]
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
