(ns pi.handlers.http
  (:require [org.httpkit.server           :as kit]
            [ring.middleware.defaults     :refer :all]
            [ring.middleware.anti-forgery :as ring-anti-forgery]
            [crypto.password.scrypt       :as pw]
            [environ.core                 :refer [env]]
            (compojure [core              :refer [defroutes GET POST]]
                       [route             :as route])
            [pi.models.core               :refer [all-users ->User]]
            [pi.views.layout              :as layout]
            [pi.handlers.chsk :refer [ring-ajax-get-ws ring-ajax-post]]))

(defn register! [ring-request]
  (let [{:keys [session params]} ring-request
        {:keys [user-id password]} params
        hash-pass (pw/encrypt password)]
    (if (get @all-users user-id)
      {:status 401}
      (dosync
        (ref-set all-users
                 (assoc @all-users user-id
                        (->User user-id
                                hash-pass
                                {:latitude nil :longitude nil}
                                nil)))
        {:status 200 :session (assoc session :uid user-id)}))))

(defn login! [ring-request]
  (let [{:keys [session params]} ring-request
        {:keys [user-id password]} params]
    (dosync
      (let [user (get @all-users user-id)
            good (if user (pw/check password (:password user)) false)]
        (if good
          {:status 200 :session (assoc session :uid user-id)}
          {:status 401})))))

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
  (let [defaults  site-defaults
        defaults* (assoc-in defaults [:security :anti-forgery]
                           {:read-token custom-get-token})]
    (wrap-defaults routes defaults*)))

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
