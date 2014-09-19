(ns pi.components.runway
  (:require [om.core         :as om
                             :include-macros true]
            [om.dom          :as dom
                             :include-macros true]
            [pi.components.nav :refer [navbar]]
            ))

(defn runway-view [app owner]
  (reify
    om/IRenderState
    (render-state [this state]
      (dom/div #js {:className "jumbotron"}
        (om/build navbar app)
        (dom/div #js {:className "image-banner"}
          (dom/span nil "Photo of your part of the world, from space, taken at night."))
        (dom/div #js {:className "image-banner"}
          (dom/span nil "Photo of your city, from space, taken at night."))
        (dom/div #js {:className "local-demo"}
          (dom/span nil "Local view in demo mode."))))))

