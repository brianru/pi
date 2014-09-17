(ns pi.components.nav
  (:require [om.core         :as om
                             :include-macros true]
            [om.dom          :as dom
                             :include-macros true]
            [clojure.string :refer [blank?]]
            ))

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
