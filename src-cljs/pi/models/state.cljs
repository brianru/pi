(ns pi.models.state)

(def app-state
  (atom
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
            :path "/app"
            :active false
            :side :left
            :restricted true}
           {:name "Teleport"
            :path "/app/teleport"
            :active false
            :side :left
            :restricted true}]
     :max-id 0
     :initialized false
     :location {:latitude 90
                :longitude 0}
     :post ""
     :username ""
     ;; TODO remove initial message
     :messages (list {:id nil
                      :time nil
                      :msg  "I can talk!"
                      :author "Duudilus"
                      :location {:latitude 90
                                 :longitude 0}
                      :distance 0.00})}))
