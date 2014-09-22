(ns pi.models.state)

(def app-state
  (atom
    {:initialized false
     :user {:uid ""
            :location {:latitude 90
                       :longitude 0}}
     :nav [{:name "Register"
            :path "/register"
            :active false
            :side :right
            :restricted false}
           {:name "Login"
            :path "/login"
            :active false
            :side :right
            :restricted false}
           {:name :username
            :path "/account"
            :active false
            :side :right
            :restricted true}
           {:name "Logout"
            :path "/logout"
            :active false
            :side :right
            :restricted true}
           {:name "Local"
            :path "/local"
            :active false
            :side :left
            :restricted true}
           {:name "Teleport"
            :path "/teleport"
            :active false
            :side :left
            :restricted true}]
     :notifications [] ;; {:path "/" :name "Alert!"}
     :max-id 0
     :location {:latitude nil
                :longitude nil}
     :post ""
     :messages (list) 
     :teleport {:location {:latitude 90
                           :longitude 0}
                :messages (list)}
     }))
