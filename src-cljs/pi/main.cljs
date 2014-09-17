(ns pi.main
  (:require [pi.models.state :refer [app-state]]
            [pi.components.core :refer [landing-view messages-view]]
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

;; Do these have to be separate functions?
;; Useful if I switch up app-state, but idk if that's necessary.
(defn page [component]
  (render-page component app-state app-container))

(defroute "/" [] (page landing-view))
(defroute "/app" [] (page messages-view))

(defn refresh-navigation []
  (let [token (.getToken history)
        set-active (fn [nav]
                     (assoc nav :active (= (:path nav) token)))]
    (swap! app-state
           (fn [x] (assoc x :nav (map set-active (:nav x)))))))
 
(defn on-navigate [event]
  (refresh-navigation)
  (secretary/dispatch! (.-token event)))

(def history (History.))
 
(doto history
  (goog.events/listen EventType/NAVIGATE on-navigate)
  (.setEnabled true))

;(let [h (History.)]
;    (goog.events/listen h EventType/NAVIGATE
;                        #(secretary/dispatch! (.-token %)))
;    (doto h (.setEnabled true)))

(defn start! []
  (start-router!))

(start!)
