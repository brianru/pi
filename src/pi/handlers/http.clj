(ns pi.handlers.http
  (:require [org.httpkit.server           :as kit]
            [ring.middleware.defaults]
            [ring.middleware.anti-forgery :as ring-anti-forgery]
            [environ.core                 :refer [env]]
            (compojure [core              :refer [defroutes GET POST]]
                       [route             :as route])
            [pi.views.layout              :as layout]
            [pi.handlers.chsk :refer [ring-ajax-get-ws ring-ajax-post]]
  ))

(defn login! [ring-request]
  (let [{:keys [session params]} ring-request
        {:keys [user-id]} params]
    {:status 200 :session (assoc session :uid user-id)}))

(defn logout! [ring-request]
  (let [{:keys [session params]} ring-request
        {:keys [user-id]} params]
    (println user-id)
    {:status 200 
     :session (assoc session :uid "")}))

(defroutes routes
  (GET  "/"        req (layout/app))
  (POST "/login"   req (login! req))
  (POST "/logout"  req (logout! req))

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
  (let [port (Integer. (or (env :port) "9899"))
        s    (kit/run-server (var my-ring-handler) {:port port})]
    (reset! server_ s)
    (println "Http-kit server is running on port" port)))
