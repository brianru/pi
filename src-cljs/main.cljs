(ns main
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [taoensso.sente :as s]
            [cljs.core.async :refer [put! chan <! sliding-buffer]]
            [geo.core :as geo]))

(enable-console-print!)

;; setup web socket handlers
(let [{:keys [chsk ch-recv send-fn state]}
      (s/make-channel-socket! "/ws" {:type :auto})]
  (def chsk       chsk)
  (def ch-chsk    ch-rev)
  (def chsk-send! send-fn)
  (def chsk-state state))

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

;; FIXME refactor
;(defmulti event-msg-handler :id) ; Dispatch on event-id
;#+cljs
;(do ; Client-side methods
;  (defmethod event-msg-handler :default ; Fallback
;    [{:as ev-msg :keys [event]}]
;    (logf "Unhandled event: %s" event))
;
;  (defmethod event-msg-handler :chsk/state
;    [{:as ev-msg :keys [?data]}]
;    (if (= ?data {:first-open? true})
;      (logf "Channel socket successfully established!")
;      (logf "Channel socket state change: %s" ?data)))
;
;  (defmethod event-msg-handler :chsk/recv
;    [{:as ev-msg :keys [?data]}]
;    (logf "Push event from server: %s" ?data))
;
;  ;; Add your (defmethod handle-event-msg! <event-id> [ev-msg] <body>)s here...
;  )


(defn- now [] 
  (quot (.getTime (js/Date.)) 1000))

(def app-state (atom {:max-id 0
                      :location {:latitude 0
                                 :longitude 0}
                      :post ""
                      :username "Señor Tester"
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
  (let [msg (-> (om/get-node owner "new-post") .-value)
        author (:username @app)
        loc (:location @app)
        post {:msg msg :author author :location loc}]
    (when post
      (om/transact! app :messages #(conj % post))
      (chsk-send! [:submit/post post])
      (om/set-state! owner :post ""))))

(defn locateMe [locate]
  (if (.hasOwnProperty js/navigator "geolocation")
    (.getCurrentPosition js/navigator.geolocation
                         #(put! locate (parse-location %)))))

(defn message-view [message owner]
  (reify
    om/IRenderState
    (render-state [this _]
      (dom/div #js {:className "row message"}
        (dom/div #js {:className "row"}
          (dom/div #js {:className "col-md-4"} (:msg message)))
        (dom/div #js {:className "row"}
          (dom/div #js {:className "col-md-2"} (:author message))
          ;; FIXME display distance, not location.
          ;; must access global state
          (dom/div #js {:className "col-md-2 col-md-offset-8"}
                    (display-location (:location message))))))))

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
      (dom/div #js {:className "container"}
        (dom/h2 nil (display-location (:location app)))
        (dom/div nil
          (dom/textarea #js {:ref "new-post"
                             :className "form-control"
                             :placeholder "What's happening in the neighborhood?"
                             :rows "3"
                             :value (:post state)
                             :onChange #(handle-change % owner state)})
          (dom/div #js {:className "row"}
            (dom/div #js {:className "col-md-2"} (:username app))
            (dom/div #js {:className "col-md-2 col-md-offset-8"}
              (dom/button #js {:type "button"
                               :className "btn btn-primary"
                               :onClick #(submit-post app owner)}
                          "Submit"))))
        (apply dom/div #js {:className "message-list"}
               (om/build-all message-view  (:messages app)
                             {:init-state state}))
                 ))))

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
