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

(defn register [app owner]
  nil)

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
            (set! (.-hash js/window.location) "/local")
            (s/chsk-reconnect! chsk)
            )
          (println "failed to login:" ajax-resp))))))

(defn logout [app owner]
  (let [{:keys [uid csrf-token]} @chsk-state]
    (s/ajax-call "/logout"
      {:method :post
       :params {:user-id uid
                :csrf-token csrf-token}}
      (fn [{:keys [?status] :as ajax-resp}]
        (if (= ?status 200)
          (do
            (om/transact! app :username (fn [_] ""))
            (set! (.-hash js/window.location) "/")
            (s/chsk-reconnect! chsk))
        (println "failed to logout:" ajax-resp))))))

(defn handle-change [e owner]
  (om/set-state! owner :post (.. e -target -value)))

(defn locateMe [locate]
  (if (.hasOwnProperty js/navigator "geolocation")
    (.getCurrentPosition js/navigator.geolocation
                         #(put! locate (util/parse-location %)))))

(defn nav-item [{:keys [name path active side restricted]} owner]
  (reify
    om/IRenderState
    (render-state [this state]
      (dom/li #js {:className (if active "active" "")}
        (dom/a #js {:href (str "#" path)} 
               (or (om/get-state owner name) name))))))

(defn nav-for [{:keys [nav username]} side]
  (filter (fn [itm] (and (= (:side itm) side)
                         (= (:restricted itm) (-> username blank? not))))
          nav))

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
        ;; FIXME not the right way to pass state to a component
              (om/build-all nav-item (nav-for app :left) 
                            {:init-state {:username (:username app)}}))
            (apply dom/ul #js {:className "nav navbar-nav navbar-right"}
        ;; FIXME not the right way to pass state to a component
              (om/build-all nav-item (nav-for app :right) 
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
                          "Submit"))))
        (dom/div #js {:className "logout"}
          (dom/button #js {:type "button"
                           :class "btn btn-primary"
                           :onTouch #(logout app owner)
                           :onClick #(logout app owner)}
                      "Logout"
                           ))))))

(defn message-view [message owner]
  (reify
    om/IRenderState
    (render-state [this _]
      (dom/div #js {:className "row message"}
        (dom/div #js {:className "row top-row"}
          (dom/div #js {:className "col-xs-8 col-md-8"}
                   (get message :msg))
          (dom/div #js {:className "col-xs-4 col-md-4"}
                   (util/format-timestamp (get message :time))))
        (dom/div #js {:className "row bottom-row"}
          (dom/div #js {:className "col-xs-6 col-md-2"}
                   (get message :author))
          (dom/div #js {:className "col-xs-6 col-md-2 col-md-offset-8"}
                   (util/format-km (get message :distance))))))))

(defn submit-post [app owner]
  (let [msg  (om/get-state owner :post)
        post {:msg msg
              :author (get @app :username)
              :location (get @app :location)}]
    (when msg
      (chsk-send! [:submit/post post])
      (om/set-state! owner :post "")
      )))

(defn new-post [app owner]
  (reify
    om/IInitState
    (init-state [_]
      {:post ""})

    om/IRenderState
    (render-state [this {:keys [post] :as state}]
      (let [username (get app :username)
            has-access (-> username blank? not)]
        (dom/div #js {:className "new-post"}
          (dom/textarea #js {:ref "new-post"
                             :className "form-control"
                             :placeholder "What's happening?"
                             :disabled (not has-access)
                             :rows "3"
                             :value post
                             :onChange #(handle-change % owner)})
          (dom/div #js {:className "row"}
            (dom/div #js {:className "pull-left"} username)
            (dom/div #js {:className "pull-right"}
              (dom/button #js {:type "button"
                               :disabled (or (not has-access)
                                             (blank? post))
                               :className "btn btn-primary"
                               :onTouch #(submit-post app owner)
                               :onClick #(submit-post app owner)}
                          "Submit"))))))))

(defn local-view [app owner]
  (reify
    om/IInitState
    (init-state [_]
      {:locate (chan (sliding-buffer 3))})
    om/IWillMount
    (will-mount [_]
      (let [locate (om/get-state owner :locate)]
        (go (loop []
              (let [new-loc (<! locate)
                    old-loc (:location @app)]
                (when-not (= old-loc new-loc)
                  (om/transact! app :location #(merge % new-loc))
                  (chsk-send! [:update/location
                               {:username (:username @app)
                                :location (:location @app)}])))
              (recur))))
      (let [locate (om/get-state owner :locate)]
        (locateMe locate) ;; init
        ;; refresh every minute TODO backoff
        (js/setInterval #(locateMe locate) 60000))
      )

    om/IRenderState
    (render-state [this state]
      (dom/div #js {:className "container"}
        (om/build navbar (select-keys app [:username :nav]))
        (dom/h4 nil (util/display-location (:location app)))
        ;; FIXME not the right way to pass state to a component
        (om/build new-post app
                  {:init-state
                   (select-keys app [:location :username])})
        (apply dom/div #js {:className "message-list"}
               (om/build-all message-view (get app :messages)))))))

(comment
(defn local-view [app owner]
  (reify
    omIRenderState
    (render-state [this state]
      (dom/div nil
        (om/build header-view app {:init-state state})
        (om/build local-view app {:init-state state})
        (om/build footer-view app {:init-state state})))))
  )
