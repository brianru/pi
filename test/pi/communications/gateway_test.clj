(ns pi.communications.gateway-test
  (:require [clojure.test :refer :all]
            [com.stuartsierra.component :as component]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop]
            [pi.generators-test :refer [user-gen]]
            [pi.communications.gateway :refer :all]
            [pi.data.datomic :refer [database]]))

(defn- fresh-system []
  (component/system-map
   :db (database "localhost" 4334)
   :gateway (component/using (gateway) [:db])))

(deftest start-stop
  (let [sys (component/start-system (fresh-system))]
    (is ((complement nil?) (-> sys :gateway :fns))) 
    (let [sys (component/stop-system sys)]
      (is (nil? (-> sys :gateway :fns))))))


;; Perform fns
