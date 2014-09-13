(ns main)

(def i (js/$ "#i"))
(def history (js/$ "#history"))
(def location (js/$ "#location"))

(defn- now [] 
  (quot (.getTime (js/Date.)) 1000))

(defn- set-loc [x]
  (.log js/console x)
  (.text location (str "lat: " js/x.coords.latitude ", "
                        "long: " js/x.coords.longitude)))

(defn- loc [] 
  (if (.hasOwnProperty js/navigator "geolocation")
    (.getCurrentPosition js/navigator.geolocation set-loc)))

(loc)

(def max-id (atom 0))

(defn add-msg [msg]
  (let [t (str "<span class=\"time\">" (- (now) (.-time msg))  "s ago</span>")
        author (str "<span class=\"author\">" (.-author msg) "</span>: ")]
    (.append history (str "<li>" author (.-msg msg) t "</li>"))))

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
             (swap! max-id #(.-id msg))))))))

(defn reset-input [i]
  (.val i ""))

(defn send-to-server []
  (let [msg (.trim js/$ (.val i))
        author (.trim js/$ (.val (js/$ "#name")))
        ; location
        payload (js-obj "msg" msg "author" author)]
    (js/console.log msg author)
    (if msg
      (do
        (.send conn (.stringify js/JSON payload))
        (reset-input i)
        ))))

(.click (js/$ "#send") send-to-server)

(.keyup (.focus i) 
  (fn [e]
    (if (= (.-which e) 13) (send-to-server))))
