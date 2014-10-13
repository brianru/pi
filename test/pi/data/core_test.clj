(ns pi.data.core-test
  (:require [clojure.test :refer :all]
            [clojure.test.check.generators :as tc]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop]
            [clojure.test.check.clojure-test :refer [defspec]]
            [pi.util :as util]
            [pi.generators-test :refer :all]
            [pi.data.core :refer :all]
            ))

(defspec furthest-was-included
  1000
  (prop/for-all [[msgs loc] msgs-loc-gen]
    (some #(.equals (furthest-message msgs loc) %) msgs)))

(defspec furthest-is-correct
  1000
  (prop/for-all [[msgs loc] msgs-loc-gen]
    (let [furthest  (furthest-message msgs loc)
          msg-locs  (map :location msgs)
          msg-dists (map #(util/distance loc %) msg-locs)
          furthest-distance (util/distance loc (:location furthest))
          max-dist  (apply max msg-dists)]
      (== furthest-distance max-dist))))

(defspec bounded-radius ;; closest <= radius <= furthest
  1000
  (prop/for-all [[msgs loc] msgs-loc-gen
                 lookback gen/nat]
    (let [radius  (calc-radius msgs loc lookback)
          min-msg (closest-message msgs loc)
          min-dst (util/distance loc (:location min-msg))
          max-msg (furthest-message msgs loc)
          max-dst (util/distance loc (:location max-msg))]
  (<= min-dst radius max-dst))))

(defspec in-radius
  1000
  (prop/for-all [radius gen/pos-int
                 loc1   coordinate-gen
                 loc2   coordinate-gen]
    (= (<= (util/distance loc1 loc2) radius)
       (in-radius? radius loc1 loc2))))

(defspec big-enough-radius ;; # of msgs >= (min (count msgs) 50)
  1000
  (prop/for-all [[msgs loc] msgs-loc-gen]
    (let [local-msgs (local-messages loc msgs)]
      (>= (count local-msgs) (min 50 (count msgs))))))

(defspec shrinking-radius ;; add a msg within the radius, radius should shrink
  1000
  (prop/for-all [[msgs loc msg*] msgs-loc-msg*-gen]
    (let [radius  (calc-radius msgs loc)
          radius* (calc-radius (cons msg* msgs) loc)]
      (if (< (count msgs) 50)
        (== radius* radius)
        (< radius* radius)))))

(test #'shrinking-radius)

(defspec focused-radius ;; add a msg outside the radius, radius should not change
  1000
  nil)

(defspec full-radius ;; can I get fewer than 50 messages by changing the lookback period?
  1000
  nil)
(defspec empty-radius ;;no msgs
  1000
  nil)
