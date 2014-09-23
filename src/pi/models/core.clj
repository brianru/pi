(ns pi.models.core
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


(defn calc-radius
  "What matters is not just the number of messages within a radius,
  but also, their timeliness.

  Therefore, we start by restricting our work to recent messages.

  You could imagine a graph where x is the radius, y is the # of
  messages per hour in that last d, and there is a separate line for
  every value of d.

  Each line is always increasing, but each line is also very different.
  Some days are slower than others.

  For this reason the choice of d, in this case fixed to a single value,
  has a great impact on the quality of the result.
  "
  ([msgs loc]
   (calc-radius msgs loc 3))
  ([msgs loc d]
   (dosync
     (let [recent (filter #(->> %
                                :time
                                t-coerce/from-long
                                (t/after? (-> d t/days t/ago)))
                          msgs)]
       (cond
         (and (>= 50 (count msgs)) (<  50 (count recent)))
         (do
           (println "recursing")
           (calc-radius msgs loc (* d 2))
           )

         :else
         (let [set-distance! #(assoc % :distance
                                 (util/distance loc (:location %)))
               calcd  (map set-distance! recent)
               sorted (sort-by :distance calcd)
               max-msg (first (max-key :distance sorted))]
           (:distance max-msg)))
       ))))

(defn in-radius?
  [radius loc1 loc2]
  (println "in-radius? " radius)
  (if (and (util/coordinate? loc1) (util/coordinate? loc2))
    (<= (util/distance loc1 loc2) radius)))

(defn local-messages [loc msgs]
  (let [radius (calc-radius msgs loc)]
    (->> msgs
         (filter #(in-radius? radius loc (:location %)))
         (sort-by :id >))))

(defn local-users [loc msgs users]
  (let [radius (calc-radius msgs loc)]
    (filter #(in-radius? radius loc (:location %)) users)))
