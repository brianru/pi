(ns pi.handlers.http
  (:require [org.httpkit.server           :as kit]
            [ring.middleware.defaults]
            [ring.middleware.anti-forgery :as ring-anti-forgery]
            [environ.core                 :refer [env]]
            (compojure [core              :refer [defroutes GET POST]]
                       [route             :as route])
            [pi.views.layout              :as layout]
            [pi.handlers.chsk :refer [ring-ajax-get-ws ring-ajax-post]]
            [hiccup.core                  :refer :all]
  ))

(defn login! [ring-request]
  (let [{:keys [session params]} ring-request
        {:keys [user-id]} params]
    {:status 200 :session (assoc session :uid user-id)}))

(defn landing-page [req]
  (layout/common
    [:p "Hello world!"]))

(defroutes routes
  (GET  "/"        req (layout/app))
  (GET  "/ext"     req (landing-page req))
  (POST "/login"   req (login! req))

  ;; These two connect the http and chsk servers.
  (GET  "/chsk"    req (#'ring-ajax-get-ws req))
  (POST "/chsk"    req (#'ring-ajax-post req))

  (route/files "" {:root "resources/public"})
  (route/not-found "<p>Page not found.</p>"))

(def my-ring-handler
  (let [ring-defaults-config
        (assoc-in ring.middleware.defaults/site-defaults
          [:security :anti-forgery]
          {:read-token (fn [req] (-> req :params :csrf-token))})]
   (ring.middleware.defaults/wrap-defaults routes
                                           ring-defaults-config)))

(defonce server_ (atom nil))
(defn stop-server! []
  (when-let [stop-f @server_]
    (stop-f :timeout 100)))

(defn start-server! []
  (stop-server!)
  (let [port (read-string (or (env :port) "9899"))
        s    (kit/run-server (var my-ring-handler) {:port port})]
    (reset! server_ s)
    (println "Http-kit server is running on port" port)))
