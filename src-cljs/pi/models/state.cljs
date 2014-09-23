(ns pi.models.state)

(def app-state
  (atom
    {:initialized false
     :user {:uid ""
            :location {:latitude nil
                       :longitude nil}}
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
     :location {:latitude nil
                :longitude nil}
     :post ""
     :messages (list) 
     :max-mid 0 ;; this is max mid the client has seen
     :teleport {:place    ""
                :location {:latitude nil
                           :longitude nil}
                :messages (list)}
     }))
