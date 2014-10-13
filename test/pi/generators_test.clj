(ns pi.generators-test
  (:require [clojure.test.check.generators :as gen]
            [geo.core :as geo]
            [pi.util :as util]
            [pi.data.core :refer :all]))

(def coordinate-gen
  (gen/fmap (partial apply ->Coordinate)
            (gen/tuple
              (gen/choose -90.0 90.0) ;; latitude
              (gen/choose -90.0 90.0) ;; longitude
              )))

(def user-gen
  (gen/fmap (partial apply ->User)
            (gen/tuple
              gen/string-ascii ;; uid
              (gen/not-empty   ;; password
                gen/string-alpha-numeric)
              coordinate-gen   ;; location
              )))

(def message-gen
  (gen/fmap (partial apply ->Message)
            (gen/tuple
              gen/pos-int      ;; mid
              gen/string-ascii ;; uid
              gen/string-ascii ;; msg
              gen/pos-int      ;; time
              coordinate-gen   ;; location
              )))

(def vote-gen
  (gen/fmap (partial apply ->Vote)
            (gen/tuple
              gen/pos-int      ;; vid
              gen/pos-int      ;; mid
              gen/string-ascii ;; uid
              gen/pos-int      ;; time
              coordinate-gen   ;; location
              )))

(def comment-gen
  (gen/fmap (partial apply ->Comment)
            (gen/tuple
              gen/pos-int      ;; cid
              gen/pos-int      ;; mid
              gen/string-ascii ;; uid
              gen/string       ;; cmt
              gen/pos-int      ;; time
              coordinate-gen   ;; location
              )))

(def msgs-loc-gen
    (gen/tuple
      (gen/not-empty (gen/list message-gen))
      coordinate-gen))

;; return a tuple of msgs loc msg*
(def msgs-loc-msg*-gen
  (gen/bind msgs-loc-gen
            (fn [[msgs loc]]
              (gen/tuple
                (gen/return msgs)
                (gen/return loc)
                (gen/return (closest-message msgs loc))))))
