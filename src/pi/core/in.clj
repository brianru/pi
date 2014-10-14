(ns pi.core.in
  (:require [com.stuartsierra.component :as component]
            [datomic.api :refer [q db] :as d]
            [pi.util :as util]
            [clojure.core.async :refer [chan <! >! pub sub]]))

(defn perform-input [conn [subject predicate object payload]]
  (let [db-fn (util/mkeyword [subject predicate] object)]
    (d/transact conn [[db-fn payload]])))
