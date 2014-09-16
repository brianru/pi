(ns pi.routes.landing
  (:require [pi.views.layout :as layout]
            [hiccup.core]))


(defn landing-page [req]
  (layout/common
    (println req)))

(defn app-page [req]
  (layout/common
    ))
