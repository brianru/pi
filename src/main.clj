(ns main
  (:gen-class)
  (:require [org.httpkit.server :refer :all]
            ; TODO would ring middleware file-info and json help?
            [clojure.data.json :as json]
            (compojure [core :refer [defroutes GET POST]]
                       [route :refer [files not-found]]
                       [handler :refer [site]])
            ; TODO setup timbre for logging
            ))

(defn- now [] (quot (System/currentTimeMillis) 1000))

; TODO what's an atom?
(def clients (atom {}))

(let [max-id (atom 0)]
  (defn next-id []
    (swap! max-id inc)))

; TODO what's a ref?
; TODO why defonce?
(defonce all-msgs (ref [{:id (next-id)
                         :time (now)
                         :msg "woah! I can talk!"
                         :author "dr. seuss"}]))

(defn msg-received [msg]
    ;; TODO can we get rid of read-json call using middleware?
  (let [data (json/read-str msg)]
    ;(info "message received: " data)
    (when (:msg data)
      ; throws out an id in case dosync throws an exception
      (let [data (merge data {:time (now) :id (next-id)})]
        (dosync
               (let [all-msgs* (conj @all-msgs data)
                     total     (count all-msgs*)]
                 (if (> total 100)
                   (ref-set all-msgs (vec (drop (- total 100) all-msgs*)))
                   (ref-set all-msgs all-msgs*))))))
    (doseq [client (keys @clients)]
      ;; send to all clients, clients handle filtering
      ;; TODO can we get rid of json-str call using middleware?
      (send! client (json/write-str @all-msgs)))))

(defn chat-handler [req]
  (with-channel req channel
    ;(info channel "connceted")
    ;; TODO what does this do?
    (swap! clients assoc channel true)
    ;; TODO what are these funny characters?
    (on-receive channel #'msg-received)
    (on-close channel (fn [status]
                        (swap! clients dissoc channel)
                        ;(info channel "closed, status" status)
                        ))))

;; FIXME naming wtf
(defroutes chatrootm
  (GET "/ws" [] chat-handler)
  (files "" {:root "static"})
  (not-found "<p>Page not found.</p>"))

;(defn- wrap-request-logging [handler]
;  (fn [{:keys [request-method uri] :as req}]
;    (let [resp (handler req)]
;      (info (name request-method) (:status resp)
;            (if-let [qs (:query-string req)]
;              (str uri "?" qs) uri))
;      resp)))

(defn -main [& args]
  (run-server (-> #'chatrootm site) {:port 9899}))
  ;(info "server started. http://127.0.0.1:9899"))
