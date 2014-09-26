;; NOTE
;; This namespace is full of exploratory code that is not referenced by
;; other namespaces and does not work.
;;
(ns pi.handlers.async
  (:require [clojure.core.async :refer [<! <!! chan go go-loop thread]]))

;; -> IN ->
(def submit (chan 1))
(go-loop []
         (when-let [new-data (<! submit)]
           (dosync
             (ref-set data-store (conj @data-store new-data))
             (doseq [user (local-users (:location data)
                                       @data-store
                                       (connected-users))]
               (>! increment [:new/post new-data]))))
         (recur))

(def update (chan (sliding-buffer 3)))
(go-loop []
         (when-let [new-data (<! update)]
           (ref-set data-store (assoc @data-store uid user)))
         (recur))

;; <- OUT <-
(def increment (chan 1))
(def swap (chan 1))

;; setup handlers based on a keyword

