(ns pi.util
  (:require [geo.core        :as geo]
            [taoensso.encore :refer [exp-backoff]]
            [goog.string :as gstring]
            [pi.moment]
            ))

(defn distance
  "I do not fully understand what the '4326' is all about."
  [loc1 loc2]
  (let [pt1 (geo/point 4326 (:latitude loc1) (:longitude loc1))
        pt2 (geo/point 4326 (:latitude loc2) (:longitude loc2))
        dist (geo/distance-to pt1 pt2)]
    dist))

(defn format-km [x]
  (cond
    (nil? x) x
    :else (str (gstring/padNumber x 1 2) "km")))

(defn format-timestamp [t]
  (println t)
  t)

(defn display-location [{:keys [latitude longitude]}]
  (if (and latitude longitude)
    (str "lat: " latitude ", long: " longitude)
    ""))

(defn parse-location [x]
  {:latitude js/x.coords.latitude
   :longitude js/x.coords.longitude})

;; TODO not async. figure out how to use core.async
;; TODO this should really be refactored to be made generically useful
;; TODO create a state machine with restart and kill predicate fns
;; called on the target fn's return value
(defn exp-repeater "Takes a function and a number of repetitions."
  ([f m]
   (exp-repeater f m 1))
  ([f m n]
   (exp-repeater f m n {:factor 7}))
  ([f m n opts]
   (when-not (> n m)
     (let [f* (fn [] (do (println m n)
                       (f) (exp-repeater f m (inc n) opts)))]
       (js/setTimeout f* (* (exp-backoff n opts) 100))))))

(defn from-now [t]
  (. (. js/moment unix t) fromNow))
