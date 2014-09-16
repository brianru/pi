(ns pi.models.state)

(def app-state (atom {:max-id 0
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
