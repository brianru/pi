(ns pi.main
  (:require [pi.models.state :refer [app-state]]
            [pi.components.runway :refer [runway-view]]
            [pi.components.gateway :refer [gateway-view]]
            [pi.components.local :refer [local-view]]
            [pi.components.teleport :refer [teleport-view]]
            [pi.components.nav :refer [nav-instrument]]
            [pi.handlers.chsk :refer [start-router!]]
            [om.core         :as om
                             :include-macros true]
            [secretary.core  :as secretary
                             :include-macros true
                             :refer [defroute]]
            [goog.events     :as events]
            [goog.history.EventType :as EventType]
    )
  (:import goog.History))

(enable-console-print!)
(secretary/set-config! :prefix "#")

(def app-container (. js/document (getElementById "app-container")))

(defn render-page [component state target]
  (om/root component state {:target target}))
                          ;  :instrument (fn [f cursor m]
                          ;                (om/build* nav-instrument
                          ;                           [f cursor m]))}))

(defn refresh-navigation [new-path]
  (let [set-active (fn [nav]
                     (assoc nav :active (= (:path nav) new-path)))]
    (swap! app-state
           (fn [x] (assoc x :nav (map set-active (:nav x)))))))

(defn page [component path]
  (let [r (render-page component app-state app-container)]
    (refresh-navigation path)
    r))

(defroute "/" [] (page runway-view "/"))
(defroute "/login" [] (page gateway-view "/login"))
(defroute "/logout" [] (page gateway-view "/logout"))
(defroute "/register" [] (page gateway-view "/register"))
(defroute "/account" [] (page gateway-view "/account"))
(defroute "/local" [] (page local-view "/local"))
(defroute "/teleport" [] (page teleport-view "/teleport"))
 
(defn on-navigate [event]
  (refresh-navigation)
  (secretary/dispatch! (.-token event)))
;
(def history (History.))
; 
(doto history
  (goog.events/listen EventType/NAVIGATE on-navigate)
  (.setEnabled true))

(defn start! []
  (start-router!))

(start!)
