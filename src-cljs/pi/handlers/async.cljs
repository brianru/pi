;; Client <-> Server Interface Layer
;; Client Oriented
;;
;; This namespace defines the verbs by which all state
;; moves through the system.
;;
;; These verbs are made available to every Om component.
;;
(ns pi.handlers.async
  (:require-macros
            [cljs.core.async.macros :as asyncm :refer [go go-loop]])
  (:require [cljs.core.async :as async :refer [put! chan <! >!
                                               sliding-buffer]]
            [pi.handlers.chsk :refer [chsk chsk-send! chsk-state]]
            [pi.models.state :refer [app-state]]
            ))

;; TODO what if i map ids to their path in app-state
;; instead of the noun?
;; then using get-in and assoc-in I can move around easily
(def noun-dictionary
  {:vid :votes
   :cid :comments
   :mid :messages})

(defn known-id? [tp id]
  (let [ids (map tp (get @app-state (get noun-dictionary tp)))]
    (not (some #(== id %) ids))))

(defn unseen?
  "The order of cond clauses is important.
  
  Votes and comments are on messages, so they contain message ids,
  therefore the presence of their ids must be checked before message ids.

  Comments will be voted on in the future. So the presence of a :cid
  may not be indicative of a comment.

  TODO I'm sure there's a cleaner way to do this where lookup keys
  fallback on each other.
  "
  [data]
  (cond (:vid data)
        (not (known-id? :vid (:vid data)))

        (:cid data)
        (not (known-id? :cid (:cid data)))

        (:mid data)
        (not (known-id? :mid (:mid data)))

        :else
        false))

;; FIXME sometimes this uses user location
;;       sometimes this uses teleport location
(defn assoc-distance [data]
  (let [cur-loc (get-in @app-state [:user :location])]
    (assoc data :distance (util/distance (:location data) cur-loc))))

;; -> IN ->
(def increment (chan 1) (comp (map #(assoc-distance %))
                              (filter unseen?)))
(go-loop []
   (when-let [value (<! increment)]
     (swap! app-state assoc noun
            (cons value (get @app-state noun))))
   (recur))

(def swap (chan 1) (comp (map #(assoc-distance %))))
(go-loop []
   (when-let [new-data (<! swap)]
     ;; sometimes noun is :messages, but with teleport it's
     ;; [:teleport :messages]
     (swap! app-state assoc noun new-data))
   (recur))

;; <- OUT <-
(def submit (chan 1))
(def update (chan (sliding-buffer 3)))

(defn vote-for! [mid]
  (let [msg (get-in @app-state [:messages mid])
        msg* (assoc msg :votes (inc (:votes msg)))]
    (swap! app-state assoc-in [:messages mid] msg*)))

;; setup handlers based on a keyword
