(ns main
  (:require-macros
            [cljs.core.async.macros :as asyncm :refer [go go-loop]])
  (:require [om.core         :as om
                             :include-macros true]
            [om.dom          :as dom
                             :include-macros true]
            [taoensso.sente  :as s]
            [cljs.core.async :as async :refer [put! chan <! >!
                                     sliding-buffer]]
            [secretary.core  :as secretary
                             :include-macros true
                             :refer [defroute]]
            [goog.events     :as events]
            [goog.history.EventType :as EventType]
            [geo.core        :as geo])
  (:import goog.History))

(enable-console-print!)
(secretary/set-config! :prefix "#")

(defn distance
  "TODO I don't know if these numbers are correct.
   What's the 4326 all about?
   TODO make sure both locs come in as clojure maps (or parse em)"
  [msg-loc my-loc]
 ;; TODO make sure coordinates are valid using geo helper fn
  ;{:pre [(and msg-log my-loc)]}
  (let [pt1 (geo/point 4326 (:latitude my-loc) (:longitude my-loc))
        pt2 (geo/point 4326 (:latitude msg-loc) (:longitude msg-loc))
        dist (geo/distance-to pt1 pt2)]
    (str dist "km")))

(defn display-location [{:keys [latitude longitude]}]
  (str "lat: " latitude ", long: " longitude))

(defn parse-location [x]
  {:latitude js/x.coords.latitude
   :longitude js/x.coords.longitude})

;; setup web socket handlers
(let [{:keys [chsk ch-recv send-fn state]}
      (s/make-channel-socket! "/chsk" {:type :auto})]
  (def chsk       chsk)
  (def ch-chsk    ch-recv)
  (def chsk-send! send-fn)
  (def chsk-state state))

(defmulti event-msg-handler 
  (fn [{:as ev-msg :keys [?data]}]
    (first ?data)))

;; wrapper for logging and such
(defn event-msg-handler* [{:as ev-msg :keys [id ?data event]}]
  (println "Event:" event)
  (event-msg-handler ev-msg))

(defmethod event-msg-handler :default
  [{:as ev-msg :keys [event ?data]}]
  (println ":default" ?data))

;; TODO refactor to take list of posts
(defmethod event-msg-handler :new/post
  [{:as ev-msg :keys [event ?data]}]
  (let [d (last ?data)
        post (assoc d :distance (distance (:location d)
                                             (:location @app-state)))]
    (println post)
    (if (> (:id post) (:max-id @app-state))
      (swap! app-state assoc :messages
             (conj (:messages @app-state) post)))))

(defn- now [] 
  (quot (.getTime (js/Date.)) 1000))

(def app-state (atom {:max-id 0
                      :initialized false
                      :location {:latitude 90
                                 :longitude 0}
                      :post ""
                      :username ""
                      :messages [{:msg  "I can talk!"
                                  :author "Duudilus"
                                  :location {:latitude 90
                                             :longitude 0}
                                  :distance "0km"}]}))


;; Components

(defn handle-change [e owner {:keys [post]}]
  (om/set-state! owner :post (.. e -target -value)))

(defn submit-post [app owner]
  (let [msg (-> (om/get-node owner "new-post") .-value)
        author (:username @app)
        loc (:location @app)
        post {:msg msg :author author :location loc}]
    (when post
      ;; not adding to state b/c must first get ID from server
      ;; might be worth doing something different to make it feel
      ;; more responsive
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
                (om/transact! app :location #(merge % location))
                (when-not (om/get-state owner :initialized)
                  (chsk-send! [:init/messages
                               {:username (:username @app)
                                :location (:location @app)}])
                  (om/transact! app :initialized (fn [_] true)))
              (recur)))))
      (let [locate (om/get-state owner :locate)]
        (locateMe locate) ;; init
        ;; refresh every minute
        (js/setInterval #(locateMe locate) 60000)))

    om/IRenderState
    (render-state [this state]
      (dom/div #js {:className "container"}
        (dom/h2 nil (display-location (:location app)))
        (dom/div nil
          (dom/textarea #js {:ref "new-post"
                             :className "form-control"
                             :placeholder "What's happening?"
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
                             {:init-state state}))))))

(defn login [app owner]
  (let [username (-> (om/get-node owner "login-username") .-value)]
    (s/ajax-call "/login"
      {:method :post
       :params {:user-id username
                :csrf-token (:csrf-token @chsk-state)}}
      ;; handle response callback
      (fn [{:keys [?status] :as ajax-resp}]
        (if (= ?status 200)
          (do
            (om/transact! app :username (fn [_] username))
            (secretary/dispatch! "/app")
            (s/chsk-reconnect! chsk)
            ;; TODO doesn't work very well
            )
          (println "failed to login:" ajax-resp))))))

(defn landing-view [app owner]
  (reify
    om/IRenderState
    (render-state [this state]
      (dom/div nil
        (dom/h1 nil "Landing page"))
      (dom/div #js {:className "form-horizontal"
                     :role "form"}
        (dom/div #js {:className "form-group"}
          (dom/label #js {:htmlFor "inputEmail3"
                          :className "col-sm-2 control-label"}
                     "Username")
          (dom/div #js {:className "col-sm-10"}
            (dom/input #js {:type "text"
                            :ref "login-username"
                            :className "form-control"
                            :value (:username state)
                            :onKeyDown #(when (= (.-key %) "Enter")
                                          (login app owner))
                            :placeholder "Username"})))
        (dom/div #js {:className "form-group"}
          (dom/div #js {:className "col-sm-offset-2 col-sm-10"}
            (dom/button #js {:type "button"
                             :className "btn btn-primary"
                             :onClick #(login app owner)}
                        "Submit")))))))

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

;; Routing

;; TODO do I need the #'s?
;; /#/
(defroute "/" [] (page landing-view))
;; /#/app
(defroute "/app" [] (page messages-view))

(def app-container (. js/document (getElementById "app-container")))

(defn render-page [component state target]
  (om/root component state {:target target}))

;; Do these have to be separate functions?
;; Useful if I switch up app-state, but idk if that's necessary.
(defn page [component]
  (render-page component app-state app-container))

(let [h (History.)]
    (goog.events/listen h EventType/NAVIGATE
                        #(secretary/dispatch! (.-token %)))
    (doto h (.setEnabled true)))

;; INIT
(def        router_ (atom nil))
(defn  stop-router! [] (when-let [stop-f @router_] (stop-f)))
(defn start-router! []
  (stop-router!)
  (reset! router_ (s/start-chsk-router! ch-chsk event-msg-handler*)))

(defn start! []
  (start-router!))

(start!)
