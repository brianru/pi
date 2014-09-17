(ns pi.components.core
  (:require-macros
            [cljs.core.async.macros :as asyncm :refer [go go-loop]])
  (:require [pi.handlers.chsk :refer [chsk chsk-send! chsk-state]]
            [pi.util :as util]
            [clojure.string :refer [blank?]]
            ;;don't like sente here. result of using ajax with callback :(
            [taoensso.sente :as s]
            [secretary.core :as secretary]
            [om.core         :as om
                             :include-macros true]
            [om.dom          :as dom
                             :include-macros true]
            [cljs.core.async :as async :refer [put! chan <! >!
                                     sliding-buffer]]
            [sablono.core :as html :refer-macros [html]]
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
            (set! (.-hash js/window.location) "/app")
            (s/chsk-reconnect! chsk)
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

(defn nav-item [{:keys [name path active side restricted]} owner]
  (reify
    om/IRenderState
    (render-state [this state]
      (let [name (if (keyword? name)
                   (om/get-state owner name)
                   name)]
        (dom/li #js {:className (str (if active "active")
                                     (if (blank? name) "hide"))}
          (dom/a #js {:href (str "#" path)} name))))))

(defn left-nav [itms]
  (filter #(= (:side %) :left) itms))
(defn right-nav [itms]
  (filter #(= (:side %) :right) itms))
(defn navbar [app owner]
  (reify
    om/IRenderState
    (render-state [this state]
      (dom/nav #js {:className "navbar navbar-default navbar-fixed-top"
                    :role "navigation"}
        (dom/div #js {:className "container"}
          (dom/div #js {:className "navbar-header"}
            (dom/div #js {:className "navbar-brand col-sm-1"} 
              (dom/span #js {:className "glyphicon glyphicon-globe"}
                        nil)))
          (dom/div nil
            (apply dom/ul #js {:className "nav navbar-nav"}
              (om/build-all nav-item (-> app :nav left-nav) nil))
            (apply dom/ul #js {:className "nav navbar-nav navbar-right"}
              (om/build-all nav-item (-> app :nav right-nav)
                {:init-state {:username (:username app)}}))
                 ))))))

(defn landing-view [app owner]
  (reify
    om/IRenderState
    (render-state [this state]
      (dom/div #js {:className "jumbotron form-horizontal"}
        (om/build navbar app state)
        (dom/div #js {:className "container login"}
          (dom/div #js {:className "form-group"}
            (dom/label #js {:htmlFor "inputEmail3"
                            :className "col-xs-2 control-label"}
                       "Username")
            (dom/div #js {:className "col-xs-10"}
              (dom/input #js {:type "text"
                              :ref "login-username"
                              :className "form-control"
                              :value (:username state)
                              :onKeyDown #(when (= (.-key %) "Enter")
                                            (login app owner))
                              :placeholder "Username"})))
          (dom/div #js {:className "form-group"}
            (dom/div #js {:className "col-xs-offset-2 col-xs-10"}
              (dom/button #js {:type "button"
                               :className "btn btn-primary"
                               :onTouch #(login app owner)
                               :onClick #(login app owner)}
                          "Submit"))))))))

(defn message-view [message owner]
  (reify
    om/IRenderState
    (render-state [this _]
      (dom/div #js {:className "row message"}
        (dom/div #js {:className "row top-row"}
          (dom/div #js {:className "col-xs-8 col-md-8"} (:msg message))
          (dom/div #js {:className "col-xs-4 col-md-4"} (util/format-timestamp
                                                 (:time message))))
        (dom/div #js {:className "row bottom-row"}
          (dom/div #js {:className "col-xs-6 col-md-2"}
                   (:author message))
          (dom/div #js {:className "col-xs-6 col-md-2 col-md-offset-8"}
                   (util/format-km (:distance message))))))))

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
        (om/build navbar app {})
        (dom/h4 nil (util/display-location (:location app)))
        (dom/div #js {:className "new-post"}
          (dom/textarea #js {:ref "new-post"
                             :className "form-control"
                             :placeholder "What's happening?"
                             :rows "3"
                             :value (:post state)
                             :onChange #(handle-change % owner state)})
          (dom/div #js {:className "row"}
            (dom/div #js {:className "pull-left"} (:username app))
            (dom/div #js {:className "pull-right"}
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
