(ns pi.models.core
  (:require [pi.util :as util]))

(let [max-id (atom 0)]
  (defn next-id []
    (swap! max-id inc)))

(defonce all-msgs (ref [{:id (next-id)
                         :time (now)
                         :msg "woah! I can talk!"
                         :author "dr. seuss"
                         :location {:latitude 90 :longitude 0}}]))

(defonce all-users (ref [{:uid nil
                          :password nil
                          :location nil
                          }]))

(defn radius [_]
  ;; calculate distance of every msg in the last hour
  ;; 
  30.0)

(defn in-radius? [loc1 loc2]
  (if (and (util/coordinate? loc1)
           (util/coordinate? loc2))
    (< (util/distance loc1 loc2) (radius loc1))
    false))

(defn local-messages [user loc msgs]
  (->> msgs
      (filter #(in-radius? loc (:location %)))
      (sort-by :id >)))

(defn connected-users
  "Get them all, or, only those within the radius of a given location."
  ([]
   (dosync
     (filter #(contains? (:any @connected-uids) (:uid %))
             @all-users)))
  ([{:keys [latitude longitude] :as loc}]
   (dosync
     (filter (comp #(contains? (:any @connected-uids) (:uid %))
                   #(in-radius? (:location %) loc))
             @all-users))))
