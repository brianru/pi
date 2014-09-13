(ns main
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [cljs.core.async :refer [put! chan <! sliding-buffer]]
            [geo.core :as geo]))

(enable-console-print!)

(defn- now [] 
  (quot (.getTime (js/Date.)) 1000))

(defn distance
  "TODO I don't know if these numbers are correct.
   What's the 4326 all about?"
  [msg-loc]
  (let [my-loc (.-state cur-loc)
        pt1 (geo/point 4326 (:latitude my-loc) (:longitude my-loc))
        pt2 (geo/point 4326 (.-latitude msg-loc) (.-longitude msg-loc))
        dist (geo/distance-to pt1 pt2)]
    (str dist "km")))

(def app-state (atom {:max-id 0
                      :location {:latitude 0
                                 :longitude 0}
                      :post ""
                      :username ""
                      :messages []}))


(defn display-location [{:keys [latitude longitude]}]
  (str "lat: " latitude ", long: " longitude))

(defn parse-location [x]
  {:latitude js/x.coords.latitude
   :longitude js/x.coords.longitude})

(defn handle-change [e owner {:keys [post]}]
  (om/set-state! owner :post (.. e -target -value)))

(defn submit-post [app owner]
  (let [new-post (-> (om/get-node owner "new-post")
                     .-value)]
    (when new-post
      (om/transact! app :messages #(conj % new-post))
      (om/set-state! owner :post ""))))

(defn locateMe [locate]
  (if (.hasOwnProperty js/navigator "geolocation")
    (.getCurrentPosition js/navigator.geolocation
                         #(put! locate (parse-location %)))))

(defn messages-view [app owner]
  (reify
    om/IInitState
    (init-state [_]
      {:post ""
       :locate (chan (sliding-buffer 3))})

    om/IWillMount
    (will-mount [_]
      (let [locate (om/get-state owner :locate)]
        (go (loop []
              (let [location (<! locate)]
                (om/transact! app :location  #(merge % location)))
              (recur)))
      (let [locate (om/get-state owner :locate)]
        (locateMe locate) ;; init
        ;; refresh every minute
        (js/setInterval #(locateMe locate) 60000))))

    om/IRenderState
    (render-state [this state]
      (dom/div nil
        (dom/h2 nil (display-location (:location app)))
        (apply dom/ul nil
               (om/build-all message-view (:messages app)
                             {:init-state state}))
        (dom/div nil
          (dom/input #js {:type "text" :ref "new-post"
                          :value (:post state)
                          :onChange #(handle-change % owner state)})
          (dom/button #js {:onClick #(submit-post app owner)}
                      "Submit"))))))

(om/root
  messages-view app-state
  {:target (. js/document (getElementById "messages"))})

;(def i (js/$ "#i"))
;(def history (js/$ "#history"))
;(def location (js/$ "#location"))


;(defn- set-loc [x]
;  (let [lat js/x.coords.latitude
;        lon js/x.coords.longitude]
;    (swap! cur-loc #(merge % {:latitude lat :longitude lon}))
;    (.text location (str "lat: " lat ", "
;                         "long: " lon))))
;
;(defn- loc [] 
;  (if (.hasOwnProperty js/navigator "geolocation")
;    (.getCurrentPosition js/navigator.geolocation set-loc)))
;(loc)
;
;
;(defn add-msg [msg]
;  (js/console.log msg)
;  (let [t (str "<span class=\"time\">" (- (now) (.-time msg))  "s ago</span>")
;        dist (str "<span class=\"distance\">" (distance (.-location msg)) "</span>")
;        author (str "<span class=\"author\">" (.-author msg) "</span>: ")]
;    (.append history (str "<li>" author (.-msg msg) dist t "</li>"))))
;
;(def conn 
;  (js/WebSocket. (str "ws://" js/document.location.host "/ws")))
;
;(set! (.-onopen conn)
;  (fn [e]
;    (.send conn
;      (.stringify js/JSON (js-obj "command" "getall")))))
;
;(set! (.-onerror conn) 
;  (fn []
;    (js/alert "error")
;    (.log js/console js/arguments)))
;
;(set! (.-onmessage conn)
;  (fn [e]
;    (let [msgs (.parse js/JSON (.-data e))]
;      (doseq [msg msgs]
;        ;; if message is new and it's for me, then...
;         (if (> (.-id msg) (.-state max-id))
;           (do
;             (add-msg msg)
;             (swap! max-id #(.-id msg))))))))
;
;(defn reset-input []
;  (.val i ""))
;
;(defn send-to-server []
;  (let [msg (.trim js/$ (.val i))
;        author (.trim js/$ (.val (js/$ "#name")))
;        payload (js-obj "msg" msg
;                        "author" author
;                        "location" (clj->js (.-state cur-loc)))]
;    (if msg
;      (do
;        (.send conn (.stringify js/JSON payload))
;        (reset-input)
;        ))))

;(.click (js/$ "#send") send-to-server)

;(.keyup (.focus i) 
;  (fn [e]
;    (if (= (.-which e) 13) (send-to-server))))
