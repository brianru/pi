;; Client <-> Server Interface Layer
;; Client Oriented
;;
;; This namespace defines the verbs by which all state
;; moves through the system.
;;
;; These verbs are made available to every Om component.
;;
(ns pi.handlers.async
  (:require-macros
            [cljs.core.async.macros :as asyncm :refer [go go-loop]])
  (:require [cljs.core.async :as async :refer [put! chan <! >!
                                               sliding-buffer]]
            [pi.handlers.chsk :refer [chsk chsk-send! chsk-state]]))

;; -> IN ->
(def increment (chan 1))
(def swap (chan 1))

;; <- OUT <-
(def submit (chan 1))
(def update (chan (sliding-buffer 3)))

;; setup handlers based on a keyword
