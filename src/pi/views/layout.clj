(ns pi.views.layout)

(defn head []
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
                "//maxcdn.bootstrapcdn.com/bootstrap/3.2.0/css/bootstrap.min.css")])

(defn common
  [& body]
  (html5
    (head)
    [:body body]))

(defn app []
  (html5
    (head)
    [:body
     [:div#app-container
      (include-js "http://fb.me/react-0.11.1.js"
                  "js/out/goog/base.js"
                  "js/main.js")
      [:script {:type "text/javascript"} "goog.require(\"main\");"]]]))
      

           


