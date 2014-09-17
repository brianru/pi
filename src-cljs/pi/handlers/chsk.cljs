(ns pi.handlers.chsk
  (:require [pi.models.state :refer [app-state]]
            [pi.util :as util]
            [taoensso.sente  :as s]
            ))

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

;; wrapper for logging and such
(defn event-msg-handler* [{:as ev-msg :keys [id ?data event]}]
  (event-msg-handler ev-msg))

(defmethod event-msg-handler :default
  [{:as ev-msg :keys [event ?data]}]
  (println "Unhandled event:" event)
  nil)

(defmethod event-msg-handler :new/post
  [{:as ev-msg :keys [event ?data]}]
  (let [d (last ?data)
        post (assoc d :distance (util/distance (:location d)
                                             (:location @app-state)))]
    (if (> (:id post) (:max-id @app-state))
      (swap! app-state assoc :messages
             (cons post (:messages @app-state))))))

(defmethod event-msg-handler :swap/posts
  [{:as ev-msg :keys [event ?data]}]
  (let [cur-loc (:location @app-state)
        raw (last ?data)
        msgs (map #(assoc % :distance
                          (util/distance cur-loc (:location %)))
                  raw)]
    (swap! app-state assoc :messages msgs)))

;; INIT
(def        router_ (atom nil))
(defn  stop-router! [] (when-let [stop-f @router_] (stop-f)))
(defn start-router! []
  (stop-router!)
  (reset! router_ (s/start-chsk-router! ch-chsk event-msg-handler*)))

