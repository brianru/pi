(ns pi.models.state)

(def app-state (atom {:max-id 0
                      :initialized false
                      :location {:latitude 90
                                 :longitude 0}
                      :post ""
                      :username ""
                      :messages [{:msg  "I can talk!"
                                  :author "Duudilus"
                                  :location {:latitude 90
                                             :longitude 0}
                                  :distance "0km"}]}))
