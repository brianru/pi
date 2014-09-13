(ns main
  (:require [geo.core :as geo]))

(def i (js/$ "#i"))
(def history (js/$ "#history"))
(def location (js/$ "#location"))

(defn- now [] 
  (quot (.getTime (js/Date.)) 1000))

(def max-id (atom 0))
(def cur-loc (atom {:latitude nil :longitude nil}))

(defn- set-loc [x]
  (let [lat js/x.coords.latitude
        lon js/x.coords.longitude]
    (swap! cur-loc #(merge % {:latitude lat :longitude lon}))
    (.text location (str "lat: " lat ", "
                         "long: " lon))))

(defn- loc [] 
  (if (.hasOwnProperty js/navigator "geolocation")
    (.getCurrentPosition js/navigator.geolocation set-loc)))
(loc)

(defn distance [msg-loc]
  (let [my-loc (.-state cur-loc)
        pt1 (geo/point 4326 (:latitude my-loc) (:longitude my-loc))
        pt2 (geo/point 4326 (.-latitude msg-loc) (.-longitude msg-loc))
        dist (geo/distance-to pt1 pt2)]
    (str dist "km")))

(defn add-msg [msg]
  (js/console.log msg)
  (let [t (str "<span class=\"time\">" (- (now) (.-time msg))  "s ago</span>")
        dist (str "<span class=\"distance\">" (distance (.-location msg)) "</span>")
        author (str "<span class=\"author\">" (.-author msg) "</span>: ")]
    (.append history (str "<li>" author (.-msg msg) dist t "</li>"))))

(def conn 
  (js/WebSocket. (str "ws://" js/document.location.host "/ws")))

(set! (.-onopen conn)
  (fn [e]
    (.send conn
      (.stringify js/JSON (js-obj "command" "getall")))))

(set! (.-onerror conn) 
  (fn []
    (js/alert "error")
    (.log js/console js/arguments)))

(set! (.-onmessage conn)
  (fn [e]
    (let [msgs (.parse js/JSON (.-data e))]
      (doseq [msg msgs]
         (if (> (.-id msg) (.-state max-id))
           (do
             (add-msg msg)
             (js/console.log msg)
             (swap! max-id #(.-id msg))))))))

(defn reset-input []
  (.val i ""))

(defn send-to-server []
  (let [msg (.trim js/$ (.val i))
        author (.trim js/$ (.val (js/$ "#name")))
        payload (js-obj "msg" msg
                        "author" author
                        "location" (clj->js (.-state cur-loc)))]
    (if msg
      (do
        (.send conn (.stringify js/JSON payload))
        (reset-input)
        ))))

(.click (js/$ "#send") send-to-server)

(.keyup (.focus i) 
  (fn [e]
    (if (= (.-which e) 13) (send-to-server))))
