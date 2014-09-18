(ns pi.views.layout
  (:require [hiccup.page :refer [html5 include-css include-js]]))

(defn- head []
  [:head
   [:meta {:charset "utf-8"
           :http-equiv "X-UA-Compatible"
           :content "width=device-width, initial-scale=1
                    maximum-scale=1, use-scalable=no"}]
   [:title "pi"]
   [:link {:rel "icon"
           :type "image/png"
           :href="/favicon.png"}]
   (include-css "/css/main.css"
                "//maxcdn.bootstrapcdn.com/bootstrap/3.2.0/css/bootstrap.min.css")
   ])

(defn common [& body]
  (html5
    (head)
    [:body body]))

(defn test-app []
  (html5
    (head)
    [:body
     [:div#app-container
      (include-js "//fb.me/react-0.11.1.js"
                  "//cdnjs.cloudflare.com/ajax/libs/moment.js/2.8.3/moment.min.js"
                  "js/out/goog/base.js"
                  "js/main.js")
      [:script {:type "text/javascript"} "goog.require(\"pi.main\");"]]]))

(defn prod-app []
  (html5
    (head)
    [:body
     [:div#app-container
      (include-js "js/main.js"
                  "//cdnjs.cloudflare.com/ajax/libs/moment.js/2.8.3/moment.min.js")]]))
