;; Client <-> Server Resources and Authentication Communications
;;
;; 
(ns pi.server.http
  (:require [com.stuartsierra.component   :as component]
            [org.httpkit.server           :as kit]
            [ring.middleware.defaults     :refer :all]
            [ring.middleware.anti-forgery :as ring-anti-forgery]
            [crypto.password.scrypt       :as pw]
            (compojure [core              :refer [defroutes GET POST]]
                       [route             :as route])
            [pi.data.core                 :refer [all-users ->User]]
            [pi.server.views.layout       :as layout]
            ))

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
            good (if-let [cur-pass (:password user)]
                   (pw/check password cur-pass)
                   false)]
        (if good
          {:status 200 :session (assoc session :uid user-id)}
          {:status 401})))))

(defn logout! [ring-request]
  (let [{:keys [session params]} ring-request
        {:keys [user-id]} params]
    {:status 200 :session (assoc session :uid "")}))

(defn stop-server! [server]
  (when-let [stop-f @server]
    (println "stopping http server")
    (stop-f :timeout 100)))
    
(defn custom-get-token [req] (-> req :params :csrf-token))

(defrecord HttpServer [port env server chsk-server]
  component/Lifecycle
  (start [this]
    (println "starting http server")
    (stop-server! server)

    (defroutes routes
      (GET  "/"         req (if (= env "prod")
                              (layout/prod-app)
                              (layout/test-app)))
      (POST "/register" req (register! req))
      (POST "/login"    req (login! req))
      (POST "/logout"   req (logout! req))

      (GET  "/chsk"     req ((:ajax-get-ws chsk-server) req))
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
