(ns pi.components.core
  (:require-macros
            [cljs.core.async.macros :as asyncm :refer [go go-loop]])
  (:require [pi.models.state :refer [app-state]]
            [pi.handlers.chsk :refer [chsk chsk-send! chsk-state]]
            [pi.util :as util]
            ;;don't like sente here. result of using ajax with callback :(
            [taoensso.sente :as s]
            [secretary.core :as secretary]
            [om.core         :as om
                             :include-macros true]
            [om.dom          :as dom
                             :include-macros true]
            [cljs.core.async :as async :refer [put! chan <! >!
                                     sliding-buffer]]
    ))

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
            (s/chsk-reconnect! chsk)
            (secretary/dispatch! "/app")
            ;; TODO doesn't work very well
            )
          (println "failed to login:" ajax-resp))))))

(defn handle-change [e owner {:keys [post]}]
  (om/set-state! owner :post (.. e -target -value)))

(defn locateMe [locate]
  (if (.hasOwnProperty js/navigator "geolocation")
    (.getCurrentPosition js/navigator.geolocation
                         #(put! locate (util/parse-location %)))))

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
                             :onTouch #(login app owner)
                             :onClick #(login app owner)}
                        "Submit")))))))

(defn message-view [message owner]
  (reify
    om/IRenderState
    (render-state [this _]
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
                (when (and (not (:initialized @app))
                           (:username @app))
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
        (dom/h2 nil (util/display-location (:location app)))
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
                               :onTouch #(submit-post app owner)
                               :onClick #(submit-post app owner)}
                          "Submit"))))
        (apply dom/div #js {:className "message-list"}
               (om/build-all message-view  (:messages app)
                             {:init-state state}))))))

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

;; TODO should these be in the routes namespace?
(def app-container (. js/document (getElementById "app-container")))

(defn render-page [component state target]
  (om/root component state {:target target}))

;; Do these have to be separate functions?
;; Useful if I switch up app-state, but idk if that's necessary.
(defn page [component]
  (render-page component app-state app-container))

