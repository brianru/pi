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
            (dom/div #js {:className "pull-left"} (count post))
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
        (locateMe locate)))
        ;(util/exp-repeater #(locateMe locate) 7)))

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
