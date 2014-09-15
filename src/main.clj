(ns main
  (:gen-class)
  (:require [clojure.core.async           :refer [<! <!! chan go thread]]
            [clojure.core.cache           :as cache]
            [org.httpkit.server           :as kit]
            [taoensso.sente               :as s]
            [ring.middleware.defaults]
            [ring.middleware.anti-forgery :as ring-anti-forgery]
            [environ.core                 :refer [env]]
            [clojure.walk                 :refer [keywordize-keys]]
            (compojure [core              :refer [defroutes GET POST]]
                       [route             :as route])
            ; TODO setup timbre for logging
            ))

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

(defmulti event-msg-handler :id)

(defmethod event-msg-handler :default
  [{:as ev-msg :keys [event id ?data ring-req ?reply-fn send-fn]}]
  (let [session (:session ring-req)
        uid     (:uid     session)]
    (println "Unhandled event: %s" event)
    (println "uid:" uid)
    (when-not (:dummy-reply-fn (meta ?reply-fn))
      (?reply-fn {:umatched-event-as-echoed-from-from-server event}))))

(defmethod event-msg-handler :chsk/uidport-open [ev-msg] nil)
(defmethod event-msg-handler :chsk/uidport-close [ev-msg] nil)
(defmethod event-msg-handler :chsk/ws-ping [ev-msg] nil)

(defmethod event-msg-handler :submit/post
  [{:as ev-msg :keys [event id ?data ring-req ?reply-fn send-fn]}]
  (let [{:keys [msg author location] :as post} (last event)]
    (when msg
      (let [data (merge post {:time (now) :id (next-id)})]
        (dosync
          (let [all-msgs* (conj @all-msgs data)
                total     (count all-msgs*)]
            (if (> total 100)
              (ref-set all-msgs (vec (drop (- total 100) all-msgs*)))
              (ref-set all-msgs all-msgs*))))
        (doseq [uid (:any @connected-uids)]
          (println "uid:" uid)
          (chsk-send! uid [:test/echo "hello"])
          (chsk-send! uid [:new/post data]))))))

(defn login! [ring-request]
  (let [{:keys [session params]} ring-request
        {:keys [user-id]} params]
    {:status 200 :session (assoc session :uid user-id)}))

(defroutes server
  (GET  "/chsk"    req (#'ring-ajax-get-ws req))
  (POST "/chsk"    req (#'ring-ajax-post req))
  (POST "/login"   req (login! req))
  (route/resources "/")
  (route/not-found "<p>Page not found.</p>"))

(def my-ring-handler
  (let [ring-defaults-config
        (assoc-in ring.middleware.defaults/site-defaults
                  [:security :anti-forgery]
                  {:read-token (fn [req] (-> req :params :csrf-token))})]
   (ring.middleware.defaults/wrap-defaults server ring-defaults-config)))


;;;; Init

(defonce http-server_ (atom nil))
(defn stop-http-server! []
  (when-let [stop-f @http-server_]
    (stop-f :timeout 100)))

(defn start-http-server! []
  (stop-http-server!)
  (let [port (read-string (or (env :port) "9899"))
        s    (kit/run-server (var my-ring-handler) {:port port})]
    (reset! http-server_ s)
    (println "Http-kit server is running on port" port)))

(defonce    router_ (atom nil))
(defn  stop-router! [] (when-let [stop-f @router_] (stop-f)))
(defn start-router! []
  (stop-router!)
  (reset! router_ (s/start-chsk-router! ch-chsk event-msg-handler)))

(defn start! []
  (start-router!)
  (start-http-server!))

(defn -main [& args]
  (start!))
