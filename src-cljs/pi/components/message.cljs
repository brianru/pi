(ns pi.components.message
  (:require [pi.util :as util]
            [om.core         :as om
                             :include-macros true]
            [om.dom          :as dom
                             :include-macros true]))

(defn submit-vote [message owner direction]
  (let [vote {:mid      (get message :mid)
              :value    direction
              :location nil}]
    (chsk-send! [:submit/vote vote])))

(defn submit-comment [message owner]
  nil)

(defn message-view [message owner]
  (reify
    om/IInitState
    (init-state [_]
      {:vote nil})

    om/IWillMount
    (will-mount [_]
      (js/setInterval #(om/refresh! owner) 60000))

    om/IRenderState
    (render-state [this {:keys [vote]}]
      (dom/div #js {:className "row message"}
        (dom/div #js {:className "col-xs-1"}
          (dom/ul #js {:className "vote-controls"}
            (dom/li nil
              (dom/span #js
                {:className (str "glyphicon glyphicon-chevron-up"
                                 (if (= vote :up) "selected"))
                 :onClick #(submit-vote message owner :up)
                 :onTouch #(submit-vote message owner :up)}))
            (dom/li nil (get message :votes))
            (dom/li nil
              (dom/span #js
                {:className (str "glyphicon glyphicon-chevron-down"
                                 (if (= vote :down) "selected"))
                 :onClick #(submit-vote message owner :down)
                 :onTouch #(submit-vote message owner :down)}))))

        (dom/div #js {:className "col-xs-11"}
          (dom/div #js {:className "row top-row"}
            (dom/div #js {:className "col-xs-8 col-md-8"}
                     (get message :msg))
            (dom/div #js {:className "col-xs-4 col-md-4"} 
                     (util/from-now (get message :time))))

          (dom/div #js {:className "row bottom-row"}
            (dom/div #js {:className "col-xs-6 col-md-2"}
                     (get message :uid))
            (dom/div #js {:className "col-xs-6 col-md-2 col-md-offset-8"}
                     (util/format-km (get message :distance)))))
        (dom/div #js {:className "row comment"}
                 "Comment component goes here")))))
