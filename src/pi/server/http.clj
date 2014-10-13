;; Client <-> Server Resources and Authentication Communications
;;
;; 
(ns pi.server.http
  (:require [com.stuartsierra.component   :as component]
            [org.httpkit.server           :as kit]
            [ring.middleware.defaults     :refer :all]
            [ring.middleware.anti-forgery :as ring-anti-forgery]
            (compojure [core              :refer [defroutes GET POST]]
                       [route             :as route])
            [pi.server.views.layout       :as layout]
            ))

(defn register! [gateway ring-request]
  (let [{:keys [session params]} ring-request
        {:keys [user-id password]} params]
    (if ((:register gateway) user-id password)
      {:status 200 :session (assoc session :uid user-id)}
      {:status 401})))

(defn login! [gateway ring-request]
  (let [{:keys [session params]} ring-request
        {:keys [user-id password]} params]
    (if ((:login gateway) user-id password)
      {:status 200 :session (assoc session :uid user-id)}
      {:status 401})))

(defn logout! [gateway ring-request]
  (let [{:keys [session params]} ring-request
        {:keys [user-id]} params]
    ((:logout gateway) user-id)
    {:status 200 :session (assoc session :uid "")}))

(defn stop-server! [server]
  (when-let [stop-f @server]
    (println "stopping http server")
    (stop-f :timeout 100)))
    
(defn custom-get-token [req] (-> req :params :csrf-token))

(defrecord HttpServer [port env server chsk-server gateway]
  component/Lifecycle
  (start [this]
    (println "starting http server")
    (stop-server! server)

    (defroutes routes
      (GET  "/"         req (if (= env "prod")
                              (layout/prod-app)
                              (layout/test-app)))
      (POST "/register" req (register! gateway req))
      (POST "/login"    req (login!    gateway req))
      (POST "/logout"   req (logout!   gateway req))

      (GET  "/chsk"     req ((:ajax-get-ws  chsk-server) req))
      (POST "/chsk"     req ((:ajax-post-ws chsk-server) req))

      (route/files "" {:root "resources/public"})
      (route/not-found "<p>Page not found.</p>"))

    (let [defaults (assoc-in site-defaults [:security :anti-forgery]
                             {:read-token custom-get-token})
          my-ring-handler (wrap-defaults routes defaults)
          s (kit/run-server my-ring-handler {:port port})]
      (println "http server is running in" env "on port" port)
      (assoc this :server s)))

  (stop [this]
    (stop-server! server)
    (assoc this :server (atom nil))))

(defn http-server [port env]
  (map->HttpServer {:port port
                    :env  env
                    :server (atom nil)
                    :chsk-server nil}))
