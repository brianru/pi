(ns pi.handlers.http
  (:require [org.httpkit.server           :as kit]
            [ring.middleware.defaults     :refer :all]
            [ring.middleware.anti-forgery :as ring-anti-forgery]
            [environ.core                 :refer [env]]
            (compojure [core              :refer [defroutes GET POST]]
                       [route             :as route])
            [pi.models.core               :refer [all-users]]
            [pi.views.layout              :as layout]
            [pi.handlers.chsk :refer [ring-ajax-get-ws ring-ajax-post]]))

(defn register! [ring-request]
  (let [{:keys [session params]} ring-request
        {:keys [user-id password]} params]
    (dosync
      (alter all-users
             assoc @all-users user-id
                   {:uid user-id :password password :location nil})
      {:status 200 :session (assoc session :uid user-id)})))

(defn login! [ring-request]
  (let [{:keys [session params]} ring-request
        {:keys [user-id password]} params]
    (dosync
      (println @all-users)
      ;; TODO check to make sure username and password match up
      {:status 200 :session (assoc session :uid user-id)})))

(defn logout! [ring-request]
  (let [{:keys [session params]} ring-request
        {:keys [user-id]} params]
    {:status 200 :session (assoc session :uid "")}))

(defroutes routes ;; NOTE only supply port env var if deployed
  (GET  "/"        req (if (env :port)
                         (layout/prod-app)
                         (layout/test-app)))
  (POST "/register"   req (register! req))
  (POST "/login"   req (login! req))
  (POST "/logout"  req (logout! req))

  ;; These two connect the http and chsk servers.
  (GET  "/chsk"    req (#'ring-ajax-get-ws req))
  (POST "/chsk"    req (#'ring-ajax-post req))

  (route/files "" {:root "resources/public"})
  (route/not-found "<p>Page not found.</p>"))

(defn custom-get-token [req] (-> req :params :csrf-token))

(def my-ring-handler
  (let [defaults (assoc-in secure-site-defaults [:security :anti-forgery]
                           {:read-token custom-get-token})]
    (wrap-defaults routes defaults)))

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
