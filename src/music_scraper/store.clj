(ns music-scraper.store
  (:require [clojure.tools.logging :as log]
            [environ.core :refer [env]]
            [music-scraper.reddit.parse :as reddit]
            [yesql.core :refer [defqueries]]))

(defn map-post [post]
  (let [{data :data} post]
    (let [parsed-data (reddit/parse-track-data (:title data))]
      {:post-id       (:id data)
       :time          (new java.sql.Timestamp (:created_utc data))
       :media-url     (:url data)
       :artist        (:artist parsed-data)
       :track         (:track parsed-data)
       :parse-failed? (or (nil? (:artist parsed-data)) (nil? (:track parsed-data)))})))

(defqueries "sql/tracks.sql")

(def db-spec {:classname   "org.postgresql.Driver"
              :subprotocol "postgresql"
              :subname     (env :database-url)
              :user        (env :database-user)
              :password    (env :database-pass)})

(defn log-query [query args]
  (log/infof "Running Query %s with args: %s" query args)
  (try
    (log/spyf "Query Results: %s" (apply query db-spec args))
    (catch Exception e (log/error e "Query failed for args:" args))))

(defn setup []
  (log/info "Setting up store")
  (log-query #'create-tracks! nil))

(defn save-track [track]
  (log-query #'insert-track<! [(:post-id track)
                               (:time track)
                               (:media-url track)
                               (:artist track)
                               (:track track)
                               (:parse-failed? track)]))

(defn track-exists? [post-id]
  (log-query #'track-exists post-id))