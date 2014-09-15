(ns main
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [om.core         :as om
                             :include-macros true]
            [om.dom          :as dom
                             :include-macros true]
            [taoensso.sente  :as s]
            [cljs.core.async :refer [put! chan <! sliding-buffer]]
            [secretary.core  :as secretary
                             :include-macros true
                             :refer [defroute]]
            [goog.events     :as events]
            [goog.history.EventType :as EventType]
            [geo.core        :as geo])
  (:import goog.History))

(enable-console-print!)

(secretary/set-config! :prefix "#")

;; setup web socket handlers
(let [{:keys [chsk ch-recv send-fn state]}
      (s/make-channel-socket! "/ws" {:type :auto})]
  (def chsk       chsk)
  (def ch-chsk    ch-rev)
  (def chsk-send! send-fn)
  (def chsk-state state))

(defmulti event-msg-handler :id)

(defmethod event-msg-handler :default ; Fallback
  [op arg app owner]
  ;[{:as ev-msg :keys [event]}]
  (println op arg)
  ;(println "Unhandled event: %s" event))
  )

(defmethod event-msg-handler :chsk/state
  [op arg app owner]
  ;[{:as ev-msg :keys [?data]}]
  (println op arg)
  ;(if (= ?data {:first-open? true})
  ;  (println "Channel socket successfully established!")
  ;  (println "Channel socket state change: %s" ?data)))
  )

(defmethod event-msg-handler :chsk/recv
  [op arg app owner]
  (println op arg)
  ;[{:as ev-msg :keys [?data]}]
  ;(println "Push event from server: %s" ?data))
  )

;; FIXME upon receiving a msg, calculate the distance

(defn- now [] 
  (quot (.getTime (js/Date.)) 1000))

(def app-state (atom {:max-id 0
                      :location {:latitude 90
                                 :longitude 0}
                      :post ""
                      :username "Señor Tester"
                      :messages [{:msg  "I can talk!"
                                  :author "Duudilus"
                                  :location {:latitude 90
                                             :longitude 0}
                                  :distance "0km"}]}))

(defn distance
  "TODO I don't know if these numbers are correct.
   What's the 4326 all about?"
  [msg-loc my-loc]
  (println msg-loc my-loc)
  (let [pt1 (geo/point 4326 (:latitude my-loc) (:longitude my-loc))
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
      (println message)
      (dom/div #js {:className "row message"}
        (dom/div #js {:className "row"}
          (dom/div #js {:className "col-md-4"} (:msg message)))
        (dom/div #js {:className "row"}
          (dom/div #js {:className "col-md-2"} (:author message))
          (dom/div #js {:className "col-md-2 col-md-offset-8"}
                   (:distance message)))))))

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
                             {:init-state state }))))))

;; TODO enable registration / login
(defn landing-view [app owner]
  (reify
    om/IRenderState
    (render-state [this state]
      (dom/h1 nil "Landing page"))))

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

;; TODO do I need the #'s?
;; /#/
(defroute "/" [] (page landing-view))
;; /#/app
(defroute "/app" [] (page messages-view))

(def app-container (. js/document (getElementById "app-container")))

(defn render-page [component state target]
  (om/root component state {:target target}))

(defn page [component]
  (render-page component app-state app-container))

(let [h (History.)]
    (goog.events/listen h EventType/NAVIGATE #(secretary/dispatch! (.-token %)))
    (doto h (.setEnabled true)))
