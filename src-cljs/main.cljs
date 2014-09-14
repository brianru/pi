(ns main
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [taoensso.sente :as s]
            [cljs.core.async :refer [put! chan <! sliding-buffer]]
            [geo.core :as geo]))

(enable-console-print!)

;; setup web socket handlers
(let [{:keys [chsk ch-recv send-fn]}
      (s/make-channel-socket! "/ws" {} {:type :auto})]
  (def chsk chsk)
  (def ch-chsk ch-rev)
  (def chsk-send! send-fn))

(defn event-loop
  "Handle inbound events."
  [app owner]
  (go (loop []
        (let [[op arg] (<! ch-chsk)]
          (case op
            :chsk/recv (handle-event arg app owner)
            ;; other sente events go here
            ))
        (recur))))

(defn- now [] 
  (quot (.getTime (js/Date.)) 1000))

(def app-state (atom {:max-id 0
                      :location {:latitude 0
                                 :longitude 0}
                      :post ""
                      :username ""
                      :messages [{:msg  "I can talk!"
                                  :author "Duudilus"
                                  :location {:latitude 90
                                             :longitude 0}}]}))

(defn distance
  "TODO I don't know if these numbers are correct.
   What's the 4326 all about?"
  [msg-loc]
  (let [my-loc (.-state cur-loc) ;; FIXME ref om state
        pt1 (geo/point 4326 (:latitude my-loc) (:longitude my-loc))
        pt2 (geo/point 4326 (.-latitude msg-loc) (.-longitude msg-loc))
        dist (geo/distance-to pt1 pt2)]
    (str dist "km")))

(defn display-location [{:keys [latitude longitude]}]
  (str "lat: " latitude ", long: " longitude))

(defn parse-location [x]
  {:latitude js/x.coords.latitude
   :longitude js/x.coords.longitude})

(defn handle-change [e owner {:keys [post]}]
  (om/set-state! owner :post (.. e -target -value)))

(defn submit-post [app owner]
  (let [msg (-> (om/get-node owner "new-post")
                     .-value)
        ;; FIXME get author and location data
        post {:msg msg :author nil :location nil}]
    (when post
      (om/transact! app :messages #(conj % post))
      (om/set-state! owner :post ""))))

(defn locateMe [locate]
  (if (.hasOwnProperty js/navigator "geolocation")
    (.getCurrentPosition js/navigator.geolocation
                         #(put! locate (parse-location %)))))

(defn message-view [message owner]
  (reify
    om/IRenderState
    (render-state [this {:keys [msg author location]}]
      (dom/li nil
        (dom/span nil msg)
        (dom/span nil author)
        (dom/span nil location)))))

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

(comment
(defn local-view [app owner]
  (reify
    omIRenderState
    (render-state [this state]
      (dom/div nil
        (om/build header-view app {:init-state state})
        (om/build messages-view app {:init-state state})
        (om/build footer-view app {:init-state state})))))
  )

(comment
(defn external-view [app owner]
  "stuff"
  ))

(om/root
  messages-view app-state
  {:target (. js/document (getElementById "messages"))})

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
;        ;; FIXME get om state
;         (if (> (.-id msg) (.-state max-id))
;           (do
;             (add-msg msg)
;             (swap! max-id #(.-id msg))))))))

;(defn reset-input []
;  (.val i ""))

;(def i (js/$ "#i"))
;(def history (js/$ "#history"))
;
;(defn add-msg [msg]
;  (js/console.log msg)
;  (let [t (str "<span class=\"time\">" (- (now) (.-time msg))  "s ago</span>")
;        dist (str "<span class=\"distance\">" (distance (.-location msg)) "</span>")
;        author (str "<span class=\"author\">" (.-author msg) "</span>: ")]
;    (.append history (str "<li>" author (.-msg msg) dist t "</li>"))))
;
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
