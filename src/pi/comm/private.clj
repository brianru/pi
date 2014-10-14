(ns pi.comm.private
  (:require [com.stuartsierra.component :as component]
            [clojure.core.async :refer [chan go go-loop close! pub
                                        sliding-buffer]]))

;; [action noun value] -> [reaction noun value]

(def verbs {;; client -> server
            :submit '(pub (chan 1) first)
            :update '(pub (chan (sliding-buffer 3)) first)
            ;; server -> client
            :increment '(pub (chan 1) first)
            :swap '(pub (chan 1) first)})

(defrecord Private [db]
  component/Lifecycle
  (start [this]
    (println "starting verb channels")
    (merge this (into {} (map (fn [[k v]] [k (eval v)]) verbs))))
  
  (stop [this]
    (println "stopping verb channels")
    (map #(-> % last close!) this)
    (merge this (zipmap (keys verbs) (repeat nil)))))

(defn private []
  (Private. nil))
