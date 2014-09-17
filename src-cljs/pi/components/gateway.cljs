(ns pi.components.gateway
  (:require-macros
            [cljs.core.async.macros :as asyncm :refer [go go-loop]])
  (:require [taoensso.sente :as s]
            [om.core         :as om
                             :include-macros true]
            [om.dom          :as dom
                             :include-macros true]
            [secretary.core  :as secretary
                             :include-macros true]
            [pi.handlers.chsk :refer [chsk chsk-state]]
            [pi.components.nav :refer [navbar]]))

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
            (secretary/dispatch! "/local")
            ;(set! (.-hash js/window.location) "/local")
            (om/transact! app :username (fn [_] username))
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
            (secretary/dispatch! "/")
            ;(set! (.-hash js/window.location) "/")
            (s/chsk-reconnect! chsk))
        (println "failed to logout:" ajax-resp))))))


(defn gateway-view [app owner]
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
                           ))))

(defn logout-view [app owner]
  (reify
    om/IRenderState
    (render-state [this state]
      (dom/div #js {:className "logout"}
               (dom/button #js {:type "button"
                                :className "red btn btn-primary"
                                :onTouch #(logout app owner)
                                :onClick #(logout app owner)}
                           "Logout")))))
