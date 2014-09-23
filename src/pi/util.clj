(ns pi.util
  (:require [geo.core :as geo]
            [clojure.data.json :as json]
            [org.httpkit.client :as http]
            [clojure.core.async :refer [>!! <!! chan]]
            ))

;; TODO memoize
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

(defn now [] (quot (System/currentTimeMillis) 1000))

(defn geocode
  "Replace a given natural language location with its coordinates
  via an async channel as the return value.
  
  Optionally accepts the callback channel as an argument."
  ([place]
   (geocode place (chan 1)))
  ([place chan]
   (let [endpoint "https://maps.googleapis.com/maps/api/geocode/json"
         api-key  "AIzaSyAmmkhErSKLgONi0mEaNgxKtI25R5QIEeg"]
     (http/get endpoint {:query-params {:key api-key
                                        :address place}}
               #(let [res (-> %
                              :body
                              json/read-str
                              (get "results")
                              first
                              (get-in ["geometry"
                                       "location"]))
                      result {:latitude (get res "lat")
                              :longitude (get res "lng")}]
                  (>!! chan result)))
     chan)))
