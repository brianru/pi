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
     ;; {:name "Somethin to do"
     ;;  :path "Where to do it"}
     :notifications []
     :max-id 0
     :initialized false
     :location {:latitude nil
                :longitude nil}
     :post ""
     :username ""
     :messages (list) 
     }))
