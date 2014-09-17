(ns pi.util
  (:require [geo.core :as geo]
            ))

(defn distance
  "TODO I don't know if these numbers are correct.
   What's the 4326 all about?
   TODO make sure both locs come in as clojure maps (or parse em)"
  [loc1 loc2]
 ;; TODO make sure coordinates are valid using geo helper fn
  ;{:pre [(and msg-log my-loc)]}
  (let [pt1 (geo/point 4326 (:latitude loc1) (:longitude loc1))
        pt2 (geo/point 4326 (:latitude loc2) (:longitude loc2))
        dist (geo/distance-to pt1 pt2)]
    dist))

(defn coordinate? [{:keys [latitude longitude]}]
  (and (geo/latitude? latitude)
       (geo/longitude? longitude)))
