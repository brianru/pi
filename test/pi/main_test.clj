(ns pi.main-test
  (:require [clojure.test :refer :all]
            [clojure.test.check.generators :as tc]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop]
            [clojure.test.check.clojure-test :refer [defspec]]
            [pi.util :as util]
            [pi.generators-test :refer :all]
            [pi.models.core :refer :all]
            [pi.main :refer :all]))

(defspec furthest-was-included
  1000
  (prop/for-all [msgs (gen/not-empty (gen/list message-gen))
                 loc  coordinate-gen]
    (some #(.equals (furthest-message msgs loc) %) msgs)))

(defspec furthest-is-correct
  1000
  (prop/for-all [msgs (gen/not-empty (gen/list message-gen))
                 loc  coordinate-gen]
    (let [furthest  (furthest-message msgs loc)
          msg-locs  (map :location msgs)
          msg-dists (map #(util/distance loc %) msg-locs)
          furthest-distance (util/distance loc (:location furthest))
          max-dist  (first (max msg-dists))]
      (println furthest-distance max-dist)
      (== furthest-distance max-dist))))

(test #'furthest-is-correct)

(util/distance {:latitude 3 :longitude 3} {:latitude 0 :longitude 0})

(furthest-message (list (->Message 0 0 "" 0 (->Coordinate 3 1))
                        (->Message 0 0 "" 0 (->Coordinate 0 2))
                        (->Message 0 0 "" 0 (->Coordinate 3 3)))
                  (->Coordinate 0 0))

(defspec bounded-radius ;; closest <= radius <= furthest
  1000
  nil)

(defspec big-enough-radius ;; # of msgs >= (min (count msgs) 50)
  1000
  nil)

(defspec shrinking-radius ;; add a msg within the radius, radius should shrink
  1000
  nil)

(defspec focused-radius ;; add a msg outside the radius, radius should not change
  1000
  nil)

(defspec full-radius ;; can I get fewer than 50 messages by changing the lookback period?
  1000
  nil)
