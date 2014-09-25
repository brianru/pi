;; Client <-> Server Interface Layer
;; Server Oriented
;;
;; This must ONLY parse relevant data from messages based on the
;; message category, as indicated by the keyword in the first position of
;; ?data.
;;
(ns pi.handlers.chsk
  (:require [pi.util :as util]
            [taoensso.sente  :as s]
            [cljs.core.async :refer [put!]]
            [pi.handlers.async :refer [increment swap]]))

;; setup web socket handlers
(let [{:keys [chsk ch-recv send-fn state]}
      (s/make-channel-socket! "/chsk" {:type :auto})]
  (def chsk       chsk)
  (def ch-chsk    ch-recv)
  (def chsk-send! send-fn)
  (def chsk-state state))

(defmulti event-msg-handler 
  (fn [{:as ev-msg :keys [?data]}]
    (first ?data)))

(defmethod event-msg-handler :default
  [{:as ev-msg :keys [event ?data]}]
  (println "Unhandled event:" event)
  nil)

(defmethod event-msg-handler :new
  [{:keys [?data]}]
  (put! increment (last ?data)))

(defmethod event-msg-handler :swap
  [{:keys [?data]}]
  (put! swap (last ?data)))

;; INIT
(def        router_ (atom nil))
(defn  stop-router! [] (when-let [stop-f @router_] (stop-f)))
(defn start-router! []
  (stop-router!)
  (reset! router_ (s/start-chsk-router! ch-chsk event-msg-handler)))
