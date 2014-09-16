(ns pi.handlers.chsk 
  (:require [taoensso.sente     :as s]
            [clojure.core.async :refer [<! <!! chan go go-loop thread]]
            ))

(defn- now [] (quot (System/currentTimeMillis) 1000))

(let [max-id (atom 0)]
  (defn next-id []
    (swap! max-id inc)))

; TODO what's a ref?
; TODO why defonce?
(defonce all-msgs (ref [{:id (next-id)
                         :time (now)
                         :msg "woah! I can talk!"
                         :author "dr. seuss"
                         :location {:latitude 90 :longitude 0}}]))

(let [{:keys [ch-recv
              send-fn
              ajax-post-fn
              ajax-get-or-ws-handshake-fn
              connected-uids]}
      (s/make-channel-socket! {})]
  (def ring-ajax-post   ajax-post-fn)
  (def ring-ajax-get-ws ajax-get-or-ws-handshake-fn)
  (def ch-chsk          ch-recv)
  (def chsk-send!       send-fn)
  (def connected-uids   connected-uids))

(defmulti event-msg-handler :id)
(defn     event-msg-handler* [{:as ev-msg :keys [id ?data event]}]
  (println "Event:" event)
  (event-msg-handler ev-msg))

(defmethod event-msg-handler :default
  [{:as ev-msg :keys [event id ?data ring-req ?reply-fn send-fn]}]
  (let [session (:session ring-req)
        uid     (:uid     session)]
    (println "Unhandled event:" event)
    (when-not (:dummy-reply-fn (meta ?reply-fn))
      (?reply-fn {:umatched-event-as-echoed-from-from-server event}))))

(defmethod event-msg-handler :chsk/uidport-open [ev-msg] nil)
(defmethod event-msg-handler :chsk/uidport-close [ev-msg] nil)
(defmethod event-msg-handler :chsk/ws-ping [ev-msg] nil)

(defn in-radius? [user loc msg]
  (println loc msg)
  true)

;; TODO not sure if this is working right
(defmethod event-msg-handler :init/messages
  [{:as ev-msg :keys [event id ?data ring-req ?reply-fn send-fn]}]
  (if-let [uid (-> ring-req :session :uid)]
    (let [{:keys [username location]} (last event)
          msgs (filter #(in-radius? username location %) @all-msgs)]
      (map #(chsk-send! uid %) msgs))
    (println "what, why?")))

(defmethod event-msg-handler :submit/post
  [{:as ev-msg :keys [event id ?data ring-req ?reply-fn send-fn]}]
  (let [{:keys [msg author location] :as post} (last event)]
    (when msg
      (let [data (merge post {:time (now) :id (next-id)})]
        (dosync
         (ref-set all-msgs (conj @all-msgs data)))
        (doseq [uid (:any @connected-uids)]
          (chsk-send! uid [:new/post data]))))))

(defonce    router_ (atom nil))
(defn  stop-router! [] (when-let [stop-f @router_] (stop-f)))
(defn start-router! []
  (stop-router!)
  (reset! router_ (s/start-chsk-router! ch-chsk event-msg-handler*)))
