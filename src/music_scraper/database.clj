(ns music-scraper.database
  (:require [clojure.tools.logging :as log]
            [environ.core :refer [env]]
            [music-scraper.reddit.parse :as reddit]
            [yesql.core :refer [defqueries]])
  (:import (java.sql Timestamp)))

(defn map-post [post]
  (let [{data :data} post]
    (let [parsed-data (reddit/parse-track-data (:title data))]
      {:post-id       (:id data)
       :time          (new Timestamp (* 1000 (:created_utc data))) ;; convert epoch timestamp to java.sql.Timestamp
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

(defn start []
  (log/info "Setting up database")
  (log-query #'create-tracks! nil))

(defn save-track [track]
  (log-query #'insert-track<! [(:post-id track)
                               (:time track)
                               (:media-url track)
                               (:artist track)
                               (:track track)]))

(defn add-spotify-uri [track uri]
  (log-query #'update-spotify-uri [uri, (:post-id track)]))

(defn track-exists? [post-id]
  (:exists (log-query #'track-exists [post-id])))