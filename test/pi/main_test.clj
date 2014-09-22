(ns pi.tests
  (:require [clojure.test :refer :all]
            [clojure.test.check.generators :as tc]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop]
            [pi.test.generators :refer :all]
            [pi.models.core :refer [->Coordinate
                                    ->User
                                    ->Message
                                    ->Vote
                                    ->Comment]]
            [pi.main :refer :all]))

(deftest a-test
  (testing "FIXME, I fail."
    (is (= 0 1))))
