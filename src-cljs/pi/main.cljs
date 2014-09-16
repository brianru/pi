(ns pi.main
  (:require [pi.components.core :refer [page landing-view messages-view]]
            [pi.handlers.chsk :refer [start-router!]]
            [secretary.core  :as secretary
                             :include-macros true
                             :refer [defroute]]
            [goog.events     :as events]
            [goog.history.EventType :as EventType]
    )
  (:import goog.History))

(enable-console-print!)
(secretary/set-config! :prefix "#")

;; Routing

;; /#/
(defroute "/" [] (page landing-view))
;; /#/app
(defroute "/app" [] (page messages-view))

(let [h (History.)]
    (goog.events/listen h EventType/NAVIGATE
                        #(secretary/dispatch! (.-token %)))
    (doto h (.setEnabled true)))

(defn start! []
  (start-router!))

(start!)
