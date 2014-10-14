(ns pi.core.out
  (:require [com.stuartsieera.component :as component]
            [clojure.core.async :refer [chan <! >! pub sub]]))

;; https://github.com/thegeez/gin/blob/d59134fc7415da60886a375b2f6c4659ff5c9e1c/src/gin/system/database_datomic.clj#L116-L144

(defn perform-output [{:keys [db-before db-after tx-data]}]
  (let [subject ___
        predicate ___
        object ___]
    ;; use subject, object predicate to identify dbfn
    ;; perform dbfn locally, put result data onto client out channel
    ))
