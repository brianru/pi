(ns pi.components.gateway
  (:require-macros
            [cljs.core.async.macros :as asyncm :refer [go go-loop]])
  (:require [taoensso.sente :as s]
            [clojure.string :refer [blank?]]
            [om.core         :as om
                             :include-macros true]
            [om.dom          :as dom
                             :include-macros true]
            [secretary.core  :as secretary
                             :include-macros true]
            [cljs.core.async :as async :refer [put! chan <! >!]]
            [pi.handlers.chsk :refer [chsk chsk-state]]
            [pi.components.nav :refer [navbar]]))

(defn register! [app owner username password]
  (s/ajax-call "/register"
               {:method :post
                :params {:user-id username
                         :password password
                         :csrf-token (:csrf-token @chsk-state)}}
               (fn [{:keys [?status ?error ?content] :as ajax-resp}]
                 (if (= ?status 200)
                   (do
                     (om/transact! app :user #(assoc % :uid username))
                     (secretary/dispatch! "/local")
                     (s/chsk-reconnect! chsk)
                  ;   (om/set-state! owner :status {:success true
                   ;                                :message "Enjoy your Pi!"}) 
                     )
                   (om/set-state! owner :status {:success false
                                                 :message "Something went wrong. Please double check your username and password, then try again."})))))

(defn register-submit [keyval app owner]
  (when (= keyval "Enter")
    (let [username (-> (om/get-node owner "reg-username") .-value)
          password (-> (om/get-node owner "reg-password") .-value)]
      (if (and username password) ;; TODO better validation. hash here?
        (register! app owner username password)
        nil))))

(defn register-view [app owner]
  (reify
    om/IInitState
    (init-state [_]
      {:status nil})
    om/IRenderState
    (render-state [this {:keys [status] :as state}]
      (dom/div nil 
        (dom/div #js {:className "form-group"}
          (dom/div #js {:className "col-xs-offset-3 col-xs-6"}
            (dom/input #js {:type "text"
                            :ref "reg-username"
                            :className "form-control"
                            :autoFocus true
                            :onKeyDown #(register-submit (.-key %)
                                                          app owner)
                            :placeholder "Username"})))
        (dom/div #js {:className "form-group"}
          (dom/div #js {:className "col-xs-offset-3 col-xs-6"}
            (dom/input #js {:type "password"
                            :ref "reg-password"
                            :className "form-control"
                            :onKeyDown #(register-submit (.-key %)
                                                          app owner)
                            :placeholder "Password"})))
        (dom/div #js {:className "form-group"}
          (dom/div #js {:className "col-xs-offset-3 col-xs-2"}
            (dom/button #js {:type "button"
                             :className "btn btn-primary"
                             :onTouch #(register-submit "Enter"
                                                        app owner)
                             :onClick #(register-submit "Enter"
                                                        app owner)}
                            "Register"))
          (if-let [{:keys [success message]} status]
            (dom/div #js {:className (str "col-xs-4" (if success
                                                       " success-text"
                                                       " failure-text"))}
               (dom/span nil message))))))))

(defn logout [app owner]
  (let [{:keys [uid csrf-token]} @chsk-state]
    (s/ajax-call "/logout"
      {:method :post
       :params {:user-id uid
                :csrf-token csrf-token}}
      (fn [{:keys [?status] :as ajax-resp}]
        (if (= ?status 200)
          (do
            (om/transact! app :user #(assoc % :uid ""))
            (secretary/dispatch! "/")
            (s/chsk-reconnect! chsk))
        (println "failed to logout:" ajax-resp))))))


(defn logout-view [app owner]
  (reify
    om/IRenderState
    (render-state [this state]
      (dom/div #js {:className "logout"}
               (dom/button #js {:type "button"
                                :className "red btn btn-primary"
                                :onKeyDown #(when (= (.-key %) "Enter")
                                              (logout app owner))
                                :onTouch #(logout app owner)
                                :onClick #(logout app owner)}
                           "Logout")))))

(defn login! [app owner username password]
  (s/ajax-call "/login"
               {:method :post
                :params {:user-id username
                         :password password
                         :csrf-token (:csrf-token @chsk-state)}}
               (fn [{:keys [?status ?error ?content] :as ajax-resp}]
                 (if (= ?status 200)
                   (do
                     (om/transact! app :user #(assoc % :uid username))
                     (secretary/dispatch! "/local")
                     (s/chsk-reconnect! chsk)
                    ; (om/set-state! owner :status
                    ;                {:success true
                    ;                 :message "Welcome back!"}) 
                     )
                   (om/set-state! owner :status
                                  {:success false
                                   :message "Something went wrong. Please double check your username and password, then try again."})))))

(defn login-submit [keyval app owner]
  (when (= keyval "Enter")
    (let [username (-> (om/get-node owner "login-username") .-value)
          password (-> (om/get-node owner "login-password") .-value)]
      (if (and username password) ;; TODO do better validation
        (login! app owner username password)
        nil))))

(defn login-view [app owner]
  (reify
    om/IInitState
    (init-state [_]
      {:status nil})
    om/IRenderState
    (render-state [this {:keys [status] :as state}]
      (dom/div nil
        (dom/div #js {:className "form-group"}
          (dom/div #js {:className "col-xs-offset-3 col-xs-6"}
            (dom/input #js {:type "text"
                            :ref "login-username"
                            :className "form-control"
                            :autoFocus true
                            :value (:username state)
                            :onKeyDown #(login-submit (.-key %)
                                                       app owner)
                            :placeholder "Username"})))
        (dom/div #js {:className "form-group"}
          (dom/div #js {:className "col-xs-offset-3 col-xs-6"}
            (dom/input #js {:type "password"
                            :ref "login-password"
                            :className "form-control"
                            :onKeyDown #(login-submit (.-key %)
                                                       app owner)
                            :placeholder "Password"}
                            )))
        (dom/div #js {:className "form-group"}
          (dom/div #js {:className "col-xs-offset-3 col-xs-2"}
            (dom/button #js {:type "button"
                             :className "btn btn-primary"
                             :onTouch #(login-submit "Enter"
                                                     app owner)
                             :onClick #(login-submit "Enter"
                                                     app owner)}
                        "Submit"))
          (if-let [{:keys [success message]} status]
            (dom/div #js {:className (str "col-xs-4" " "
                                          (if success "success-text"
                                            "failure-text"))}
               (dom/span nil message))))))))

(defn gateway-view [app owner]
  (reify
    om/IRenderState
    (render-state [this state]
      (dom/div #js {:className "jumbotron form-horizontal"}
        (om/build navbar app)
        (dom/div #js {:className "container login"}
          (if-not (blank? (get-in app [:user :uid]))
            (om/build logout-view app )
            (dom/div nil
                    (om/build login-view app )
                    (om/build register-view app ))))))))
