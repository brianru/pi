(ns pi.handlers.async
  (:require [clojure.core.async :refer [<! <!! chan go go-loop thread]]))

;; -> IN ->
(def submit (chan 1))
(def update (chan (sliding-buffer 3)))

;; <- OUT <-
(def increment (chan 1))
(def swap (chan 1))

;; setup handlers based on a keyword

