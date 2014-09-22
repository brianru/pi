(ns pi.components.local
  (:require-macros
            [cljs.core.async.macros :as asyncm :refer [go go-loop]])
  (:require [om.core         :as om
                             :include-macros true]
            [om.dom          :as dom
                             :include-macros true]
            [cljs.core.async :as async :refer [put! chan <! >!
                                     sliding-buffer]]
            [clojure.string :refer [blank?]]
            [pi.util :as util]
            [pi.handlers.chsk :refer [chsk chsk-send! chsk-state]]
            [pi.components.nav :refer [navbar]]
            [pi.components.message :refer [message-view]]
            ))

(defn handle-change [e owner]
  (om/set-state! owner :post (.. e -target -value)))

(defn locateMe [locate]
  (if (.hasOwnProperty js/navigator "geolocation")
    (.getCurrentPosition js/navigator.geolocation
                         #(put! locate (util/parse-location %)))))

(defn submit-post [app owner]
  (let [msg  (om/get-state owner :post)
        post {:msg msg
              :author (get-in @app [:user :uid])
              :location (get-in @app [:user :location])}]
    (when msg
      (chsk-send! [:submit/post post])
      (om/set-state! owner :post "")
      )))

(defn meta-enter? [k]
  (and (.-metaKey k) (= (.-key k) "Enter")))

(defn new-post [app owner]
  (reify
    om/IInitState
    (init-state [_]
      {:post ""})
    om/IRenderState
    (render-state [this {:keys [post] :as state}]
      (println "here")
      (let [user (get app :user)
            username (:uid user)
            has-access (-> username blank? not)
            has-location (-> user :location :latitude)
            ready (and has-access has-location)]
        (dom/div #js {:className "new-post"}
          (dom/textarea #js {:ref "new-post"
                             :className "form-control"
                             :placeholder "What's happening?"
                             :disabled (not ready)
                             :rows "3"
                             :value post
                             :onKeyDown #(if (meta-enter? %)
                                           (submit-post app owner))
                             :onChange #(handle-change % owner)})
          (dom/div #js {:className "row"}
            (dom/div #js {:className "pull-left"} (count post))
            (dom/div #js {:className "pull-right"}
              (dom/button #js {:type "button"
                               :disabled (or (not ready)
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
                    old-loc (get-in @app [:user :location])]
                (when-not (= old-loc new-loc)
                  (om/transact! app :user #(assoc % :location new-loc))
                  (chsk-send! [:update/location
                               {:username (-> @app :user :uid)
                                :location (-> @app :user :location)}])))
              (recur))))
      (let [locate (om/get-state owner :locate)]
        (locateMe locate)))
        ;(util/exp-repeater #(locateMe locate) 7)))
    om/IRenderState
    (render-state [this state]
      (dom/div #js {:className "container"}
        (om/build navbar (select-keys app [:user :nav]))
        (dom/h4 nil (util/display-location (-> app :user :location)))
        (om/build new-post app {:init-state (:user app)})
        (apply dom/div #js {:className "message-list"}
               (om/build-all message-view (get app :messages)))))))
