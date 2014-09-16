(ns pi.models.db
  (:require [environ.core :refer [env]]
            [clojure.java.jdbc :as sql]))

(def db (or (env :database-url)
            "postgresql://localhost:5432/pi"))

(sql/with-db-connection [con db]
  (println (sql/query con
                      "select nspname from pg_catalog.pg_namespace;")))
