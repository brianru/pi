(ns pi.components.nav
  (:require [om.core         :as om
                             :include-macros true]
            [om.dom          :as dom
                             :include-macros true]
            [clojure.string :refer [blank?]]
            [secretary.core  :as secretary
                             :include-macros true]
            ))

(defn nav-item [{:keys [name path active side restricted]} owner]
  (reify
    om/IRenderState
    (render-state [this state]
      (dom/li #js {:className (if active "active" "")}
        (dom/a #js {:href "#"
                    :onClick #(secretary/dispatch! path)}
               (or (om/get-state owner name) name))))))

(defn nav-for [{:keys [nav username]} side]
  (filter (fn [itm] (and (= (:side itm) side)
                         (= (:restricted itm) (-> username blank? not))))
          nav))

(defn notification [data owner]
  (reify
    om/IRenderState
    (render-state [this state]
      (dom/li nil
        (dom/a #js {:href (get data :path)}
          (get data :name))))))
                   
(defn notifications [data owner]
  (reify
    om/IInitState
    (init-state [_]
      {:open false})
    om/IRenderState
    (render-state [this {:keys [open] :as state}]
      (dom/li #js {:className (str "dropdown" (if open " open"))}
        (dom/a #js {:className  "dropdown-toggle"
                    :onClick #(om/set-state! owner :open (not open)) 
                    :onTouch #(om/set-state! owner :open (not open))}
          (dom/span #js {:className "glyphicon glyphicon-flag"} nil))
        (apply dom/ul #js {:className "dropdown-menu"
                           :role "menu"}
               (om/build-all notification
                             (get data :notifications)))))))

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
              (om/build-all nav-item (nav-for app :left)
                            {:init-state {:username (:username app)}}))
            ;; TODO add notifications in the center
            (apply dom/ul #js {:className "nav navbar-nav navbar-right"}
              (if (-> (:username app) blank? not) (om/build notifications app))
              (om/build-all nav-item (nav-for app :right)
                            {:init-state {:username (:username app)}}))
                 ))))))
