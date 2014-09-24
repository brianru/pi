(ns pi.generators-test
  (:require [clojure.test.check.generators :as gen]
            [pi.models.core :refer :all]))

(def coordinate-gen
  (gen/fmap (partial apply ->Coordinate)
            (gen/tuple
              gen/pos-int ;; latitude
              gen/pos-int ;; longitude
              )))

(def user-gen
  (gen/fmap (partial apply ->User)
            (gen/tuple
              gen/nat        ;; uid
              (gen/not-empty ;; password
                gen/string-alpha-numeric)
              coordinate-gen ;; location
              )))

(def message-gen
  (gen/fmap (partial apply ->Message)
            (gen/tuple
              gen/pos-int    ;; mid
              gen/nat        ;; uid
              gen/string     ;; msg
              gen/pos-int    ;; time
              coordinate-gen ;; location
              )))

(def vote-gen
  (gen/fmap (partial apply ->Vote)
            (gen/tuple
              gen/pos-int    ;; vid
              gen/pos-int    ;; mid
              gen/pos-int    ;; uid
              gen/pos-int    ;; time
              coordinate-gen ;; location
              )))

(def comment-gen
  (gen/fmap (partial apply ->Comment)
            (gen/tuple
              gen/pos-int    ;; cid
              gen/pos-int    ;; mid
              gen/pos-int    ;; uid
              gen/string     ;; cmt
              gen/pos-int    ;; time
              coordinate-gen ;; location
              )))
