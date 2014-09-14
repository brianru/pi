(ns main
  (:gen-class)
  (:require [clojure.core.async :refer [<! <!! chan go thread]]
            [clojure.core.cache :as cache]
            [org.httpkit.server :as kit]
            [taoensso.sente :as s]
            ; TODO would ring middleware file-info and json help?
            [environ.core :refer [env]]
            [clojure.data.json :as json]
            [clojure.walk :refer [keywordize-keys]]
            (compojure [core :refer [defroutes GET POST routes]]
                       [route :refer [files not-found]]
                       [handler :refer [site]])
            ; TODO setup timbre for logging
            ))

(let [{:keys [ch-recv send-fn ajax-post-fn
              ajax-get-or-ws-handshake-fn]}
      (s/make-channel-socket! {})]
  (def ring-ajax-post ajax-post-fn)
  (def ring-ajax-get-ws ajax-get-or-ws-handshake-fn)
  (def ch-chsk ch-recv)
  (def chsk-send! send-fn))

(defn- now [] (quot (System/currentTimeMillis) 1000))

(def clients (atom {}))

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

;(defn msg-received [msg]
;    ;; TODO can we get rid of read-json call using middleware?
;  (let [data (keywordize-keys (json/read-str msg))]
;    ;(info "message received: " data)
;    (println data)
;    ;; NOTE this silently does nothing when there's no :msg key
;    (when (:msg data)
;      ; throws out an id in case dosync throws an exception
;      (let [data (merge data {:time (now) :id (next-id)})]
;        (dosync
;               (let [all-msgs* (conj @all-msgs data)
;                     total     (count all-msgs*)]
;                 (if (> total 100)
;                   (ref-set all-msgs (vec (drop (- total 100) all-msgs*)))
;                   (ref-set all-msgs all-msgs*))))))
;    (doseq [client (keys @clients)]
;      ;; send to all clients, clients handle filtering
;      ;; TODO can we get rid of json-str call using middleware?
;      (send! client (json/write-str @all-msgs)))))
;
;(defn chat-handler [req]
;  (with-channel req channel
;    ;(info channel "connceted")
;    ;; TODO what does this do?
;    (swap! clients assoc channel true)
;    ;; TODO what are these funny characters?
;    (on-receive channel #'msg-received)
;    (on-close channel (fn [status]
;                        (swap! clients dissoc channel)
;                       ;(info channel "closed, status" status)
;                        ))))

(defmulti handle-event
  "Handle events based on the event ID (keyword)."
  (fn [[ev-id ev-arg] ring-req] ev-id))

(defmethod handle-event :chsk/ping
  [event req]
  (print event req))

(defmethod handle-event :default
  [event req]
  (print event req))

(defmethod handle-event :submit/post
  [x y]
  ;[{:keys [msg author location] :as event}]
  (print x y))


(defroutes server
  (-> (routes
        (GET "/ws" req (#'ring-ajax-get-ws req))
        (POST "/ws" req (#'ring-ajax-post req))
        (files "" {:root "static"})
        (not-found "<p>Page not found.</p>"))
      site))

(defn event-loop
  "Handle inbound events."
  []
  (go (loop [{:keys [client-uuid ring-req event]} (<! ch-chsk)]
        (thread (handle-event event ring-req))
        (recur (<! ch-chsk)))))

(defn -main [& args]
  (event-loop)
  (let [port (read-string (or (env :port) "9899"))]
    (println "Starting server on port" port "...")
    (kit/run-server #'server {:port port})))
