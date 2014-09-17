(ns pi.models.state)

(def app-state
  (atom
    ; TODO this 
    ;{:user {:uid ""
    ;        :location {:latitude 90
    ;                   :longitude 0}}}
    {:nav [{:name "Register"
            :path "/register"
            :active false
            :side :right
            :restricted false}
           {:name "Login"
            :path "/"
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
     :max-id 0
     :initialized false
     :location {:latitude 90
                :longitude 0}
     :post ""
     :username ""
     :messages (list) 
     }))
