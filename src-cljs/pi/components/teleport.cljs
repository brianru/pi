(ns pi.components.teleport
  (:require [om.core         :as om
                             :include-macros true]
            [om.dom          :as dom
                             :include-macros true]
            [pi.components.nav :refer [navbar]]
            ))

(defn teleport-view [app owner]
  (reify
    om/IRenderState
    (render-state [this state]
      (dom/div #js {:className "container"}
        (om/build navbar (select-keys app [:username :nav]))
        (dom/span nil "TODO")))))
