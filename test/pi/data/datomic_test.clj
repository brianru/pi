(ns pi.data.datomic-test
  (:require [clojure.test :refer :all]
            [com.stuartsierra.component :as component]
            [pi.data.datomic :refer :all]))

(defn- fresh-db []
  (database "localhost" 4334))

(deftest start
  (is ((complement nil?) (-> (fresh-db)
                             component/start
                             :connection))))

(deftest stop
  (is (nil? (-> (fresh-db)
                component/start
                component/stop
                :connection))))

(test-ns *ns*)

;(deftest query)

;(deftest transact)

;; (deftest schema)
