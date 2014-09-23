(ns pi.handlers.chsk 
  (:require [clojure.core.cache :as cache]
            [taoensso.sente :as s]
            [clojure.string :refer [blank?]]
            [clojure.core.async :refer [<! <!! chan go go-loop thread]]
            [pi.models.core :refer [all-msgs all-users
                                    next-mid local-messages local-users]]
            ;; too much about the data model is leaking out
            [pi.util :as util]
            ))

;; Create channel for communicating with clients
;; and bind send/receive functions to local names.
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

(defn connected-users
  "Get all connected users."
  ([]
   (dosync
     (map #(get @all-users %) (:any @connected-uids)))))

(defmulti event-msg-handler :id)
(defn     event-msg-handler*
  "This is supposed to be for logging and error handling, but I have not
  found the extra level of indirection useful thusfar."
  [{:as ev-msg :keys [id ?data event]}]
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

(defn valid-user? [uid]
  (complement
    (or (nil? uid) (blank? uid))))
;; User updates their current location, which is tracked to support
;; real time updates and push notifications.
(defmethod event-msg-handler :update/location
  [{:keys [event ring-req]}]
  (let [uid (-> ring-req :session :uid)]
    (if (valid-user? uid)
      (let [{:keys [uid location]} (last event)
            user  (get @all-users uid)
            user* (assoc user :location location)
            msgs  (local-messages location @all-msgs)]
        (dosync
          (ref-set all-users (assoc @all-users uid user*))
          (chsk-send! uid [:swap/posts msgs]))))))

(defmethod event-msg-handler :submit/post
  [{:keys [event ring-req]}]
  (let [uid  (-> ring-req :session :uid)
        post (last event)]
    (when (and (:msg post) (= uid (:uid post))
               (valid-user? uid))
      (let [data (merge post {:time (util/now)
                              :mid (next-mid)})]
        (dosync
          (ref-set all-msgs (conj @all-msgs data))
          (doseq [user (local-users (:location data)
                                    @all-msgs
                                    (connected-users))]
            (chsk-send! (:uid user) [:new/post data])))))))

(defmethod event-msg-handler :update/teleport-location
  [{:keys [event ring-req]}]
  (let [uid (-> ring-req :session :uid)]
    (if (valid-user? uid)
      (let [place    (last event)
            location (<!! (util/geocode place))
            messages (local-messages location @all-msgs)]
        (chsk-send! uid [:swap-teleport/posts {:place    place
                                               :location location
                                               :messages messages}])
        ))))

(defonce    router_ (atom nil))
(defn  stop-router! [] (when-let [stop-f @router_] (stop-f)))
(defn start-router! []
  (stop-router!)
  (reset! router_ (s/start-chsk-router! ch-chsk event-msg-handler*)))
