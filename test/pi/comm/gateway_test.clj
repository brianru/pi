(ns pi.comm.gateway-test
  (:require [clojure.test :refer :all]
            [com.stuartsierra.component :as component]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop]
            [clojure.test.check.clojure-test :refer [defspec]]
            [pi.comm.gateway :refer :all]
            [datomic.api :as d]
            [pi.data.datomic :refer [database]]))

(defn- fresh-system []
  (component/system-map
   :db (database "localhost" 4334)
   :gateway (component/using (gateway) [:db])))

(deftest start-stop
  (let [sys (component/start-system (fresh-system))]
    (is ((complement nil?) (get-in sys [:gateway :fns]))) 
    (let [sys (component/stop-system sys)]
      (is (nil? (-> sys :gateway :fns))))))

(deftest register
  (let [{:keys [gateway db] :as sys} (component/start-system
                                      (fresh-system))
        [username password] ["brian" "rubinton"]
        register!  (get-in gateway [:fns :register])
        return-val (register! username password)
        res (and return-val
                 ((complement empty?)
                  (d/q '[:find ?e
                         :in $ ?username
                         :where [?e :user/name ?username]]
                       (d/db (:conn db))
                       username)))]
    (component/stop-system sys)
    (is (true? res))))

(deftest login
  (let [{:keys [gateway db] :as sys} (component/start-system
                                      (fresh-system))
        login! (get-in gateway [:fns :login])
        register! (get-in gateway [:fns :register])
        conn (:conn db)
        reg  (register! "brian" "rubinton")
        res  (login! "brian" "rubinton")]
    (component/stop-system sys)  
    (is (true? res))))

(test-ns *ns*)
