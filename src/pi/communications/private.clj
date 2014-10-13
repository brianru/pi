;; NOTE
;; This namespace is full of exploratory code that is not referenced by
;; other namespaces and does not work.
;;
(ns pi.communications.private
  (:require [com.stuartsierra.component :as component]
            [clojure.core.async :refer [chan go go-loop close!
                                        sliding-buffer]]))

;; -> IN ->
;(go-loop []
;         (when-let [new-data (<! submit)]
;           (dosync
;             (ref-set data-store (conj @data-store new-data))
;             (doseq [user (local-users (:location data)
;                                       @data-store
;                                       (connected-users))]
;               (>! increment [:new/post new-data]))))
;         (recur))
;
;(go-loop []
;         (when-let [new-data (<! update)]
;           (ref-set data-store (assoc @data-store uid user)))
;         (recur))

;; <- OUT <-

;; this could be made more generic - or at least the description can
;; be
(def verbs {;; client -> server
            :submit '(chan 1)
            :update '(chan (sliding-buffer 3))
            ;; server -> client
            :increment '(chan 1)
            :swap '(chan 1)})

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
