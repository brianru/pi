(ns pi.data.core
  (:require [pi.util :as util]
            [clj-time.coerce :as t-coerce]
            [clj-time.core :as t]))

(defrecord Coordinate [latitude longitude])

(let [max-id (atom 0)]
  (defn next-uid []
    (swap! max-id inc)))
(defrecord User [uid password location radius])
(defonce all-users (ref {}))
                        ;{"apple" {:uid "apple"
                        ;          :password nil
                        ;          :location nil
                        ;          }

(let [max-id (atom 0)]
  (defn next-mid []
    (swap! max-id inc)))
(defrecord Message [mid uid msg time location])
(defonce all-msgs (ref []))
                       ;{:id (next-id)
                       ; :time (util/now)
                       ; :msg "woah! I can talk!"
                       ; :uid "dr. seuss"
                       ; :location {:latitude 90 :longitude 0}}

(let [max-id (atom 0)]
  (defn next-vid []
    (swap! max-id inc)))
(defrecord Vote [vid mid uid time location])
(defonce all-votes (ref []))

(let [max-id (atom 0)]
  (defn next-cid []
    (swap! max-id inc)))
(defrecord Comment [cid mid uid cmt time location])
(defonce all-comments (ref []))


(defn recent? [lookback t]
  (->> t
       :time
       t-coerce/from-long
       (t/after? (-> lookback t/days t/ago))))

(defn sort-messages-by-distance
  ([msgs loc]
   (sort-messages-by-distance <))
  ([msgs loc f]
   (sort-by #(util/distance loc (:location %)) f msgs)))

(defn furthest-message [msgs loc]
  (first (sort-messages-by-distance msgs loc >)))

(defn closest-message [msgs loc]
  (first (sort-messages-by-distance msgs loc <)))

(defn calc-radius
  "What matters is not just the number of messages within a radius,
  but also, their timeliness.

  Therefore, we start by restricting our work to recent messages.

  You could imagine a graph where x is the radius, y is the # of
  messages per hour in that last d, and there is a separate line for
  every value of d.

  Each line is always increasing, but each line is also very different.
  Some days are slower than others.

  For this reason the choice of lookback period,
  in this case fixed to a single value,
  has a great impact on the quality of the result.
  "
  ([msgs loc]
   (calc-radius msgs loc 3))
  ([msgs loc lookback]
  ; (println "all msgs given to calc-radius: " (count msgs))
   (if (> (count msgs) 0)
     (dosync
       (let [recent-msgs (filter #(recent? lookback %) msgs)]
         ;(println "Recent-msgs: " (count recent-msgs))
         (cond
           (and (>= (count msgs) 50) (< (count recent-msgs) 50))
           (do ;(println "recursing" loc lookback)
               (calc-radius msgs loc (* lookback 2)))

           :else
           (util/distance loc
                  (:location (furthest-message recent-msgs loc))))))
     0.0)))

(defn in-radius?
  [radius loc1 loc2]
  (if (and (util/coordinate? loc1) (util/coordinate? loc2))
    (<= (util/distance loc1 loc2) radius)
    false))

(defn local-messages [loc msgs]
  (let [radius (calc-radius msgs loc)
        local  (filter #(in-radius? radius loc (:location %)) msgs)]
  ;  (println "radius: " radius)
;    (println "local messages: " (count local) "/" (count msgs))
    (sort-by :mid > local)))

(defn local-users [loc msgs users]
  (let [radius (calc-radius msgs loc)
        local  (filter #(in-radius? radius loc (:location %)) users)]
  ;  (println "local users: " (count local) "/" (count users))
    local))
