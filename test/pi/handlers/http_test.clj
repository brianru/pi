(ns pi.handlers.http-test
  (:require [clojure.test :refer :all]
            [com.stuartsierra.component :as component]
            [pi.handlers.http :refer :all]))

(defn- fresh-server []
  (http-server 4121 "test"))

(defrecord MockChskServer []
  component/Lifecycle
  (start [this]
    (merge this {:ajax-get-ws true
                 :ajax-post-ws true}))
  (stop [this]
    (merge this {:ajax-get-ws nil
                 :ajax-post-ws nil})))

(defn- mock-chsk-server []
  (->MockChskServer))

(defn- fresh-mock-system []
  (component/system-map
   :chsk-server (mock-chsk-server)
   :http-server (component/using (fresh-server) [:chsk-server])))

(deftest start-stop
  (let [sys (component/start-system (fresh-mock-system))]
    (is ((complement nil?) (:server sys)))
    (is (true? (-> sys :chsk-server :ajax-get-ws)))
    (let [sys (component/stop-system sys)]
      (is (nil? (:server sys)))
      (is (nil? (-> sys :chsk-server :ajax-get-ws))))))

;; ping the server

;; register
;; get a ws

(test-ns *ns*)

(deftest stop
  (is))

(def a (component/start (fresh-mock-system)))
(def b (component/stop a))
