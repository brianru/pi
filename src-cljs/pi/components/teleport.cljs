(ns pi.components.teleport
  (:require-macros
            [cljs.core.async.macros :as asyncm :refer [go go-loop]])
  (:require [taoensso.sente :as s]
            [om.core         :as om
                             :include-macros true]
            [om.dom          :as dom
                             :include-macros true]
            [clojure.string :refer [blank?]]
            [cljs.core.async :as async
                             :refer [put! chan <! >! sliding-buffer]]
            [pi.util         :as util]
            [pi.handlers.chsk :refer [chsk chsk-send! chsk-state]]
            [pi.components.nav :refer [navbar]]
            [pi.components.message :refer [message-view]]
            ))

(defn teleport! [place]
  (chsk-send! [:update/teleport-location place]))

(defn teleport-submit [app owner]
  (let [place (.-value (om/get-node owner "teleport-destination"))
        uid   (get-in @app [:user :uid])]
    (if (and (-> place blank? not) (-> uid blank? not))
      (teleport! place)
      (om/transact! app :teleport #(assoc % :place place)))))

(defn refresh [place]
  (if (-> place blank? not) (teleport! place)))

(defn enter? [key-down]
  (= (.-key key-down) "Enter"))

(defn teleport-view
  "Refreshes approximately every minute by sending the latest
  teleport location to the server, which motivates the server to send back
  the messages within range of that location."
  [app owner]
  (reify
    om/IWillMount
    (will-mount [_]
      (refresh (get-in app [:teleport :place]))
      (js/setInterval #(refresh (get-in @app [:teleport :place]))
                      10000))

    om/IRenderState
    (render-state [this state]
      (let [{:keys [place location messages]} (:teleport app)]
        (dom/div #js {:className "container"}
          (om/build navbar (select-keys app [:user :nav]))
          (dom/h4 nil place)
          (dom/h4 nil (util/display-location location))
          (dom/div #js {:className "teleport-input row"}
            (dom/div #js {:className "col-lg-6"}
              (dom/div #js {:className "input-group"}
                (dom/span #js {:className "input-group-btn"}
                  (dom/button
                    #js {:className "btn btn-default"
                         :type "button"
                         :onClick #(teleport-submit app owner)
                         :onTouch #(teleport-submit app owner)}
                    "Go!"))
                (dom/input #js {:className "form-control"
                                :ref "teleport-destination"
                                :type "text"
                                :placeholder "Where do you want to go?"
                                :onKeyDown
                                  #(if (enter? %)
                                     (teleport-submit app owner))}))))
           (apply dom/div #js {:className "message-list"}
                 (om/build-all message-view messages)))))))
