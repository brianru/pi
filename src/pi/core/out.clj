(ns pi.core.out
  (:require [com.stuartsieera.component :as component]
            [clojure.core.async :refer [chan <! >! pub sub]]))

(defn perform-output [{:keys [db-before db-after tx-data]}]
  (let [subject ___
        predicate ___
        object ___]
    ;; use subject, object predicate to identify dbfn
    ;; perform dbfn locally, put result data onto client out channel
    ))
