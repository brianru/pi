(ns pi.components.teleport
  (:require [om.core         :as om
                             :include-macros true]
            [om.dom          :as dom
                             :include-macros true]
            [pi.util         :as util]
            [pi.components.nav :refer [navbar]]
            ))

(defn handle-change [owner change]
  (let [loc (om/get-state owner :location)]
    (om/transact! app :teleport #(assoc :location loc))
    (chsk-send! [:update/teleport-location
                 {:username (-> @app :user :uid)
                  :location loc}])))

(defn teleport-view [app owner]
  (reify
    om/IRenderState
    (render-state [this state]
      (let [{:keys [location messages]} (:teleport app)]
        (dom/div #js {:className "container"}
          (om/build navbar (select-keys app [:user :nav]))
          (dom/h4 nil (util/display-location location))
          (dom/div #js {:className "row"}
            (dom/input #js {:className ""
                            :type "number"
                            :onChange #(handle-change owner %)
                            :value (:latitude location)})
            (dom/input #js {:className ""
                            :type "number"
                            :onChange #(handle-change owner %)
                            :value (:longitude location)}))
          (apply dom/div #js {:className "message-list"}
                 (om/build-all message-view messages)))))))
