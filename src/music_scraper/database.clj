(ns music-scraper.database
  (:require [clojure.tools.logging :as log]
            [environ.core :refer [env]]
            [yesql.core :refer [defqueries]]
            [com.stuartsierra.component :as component]))

(defqueries "sql/tracks.sql")

(defn log-query [db-spec query args]
  (log/infof "Running Query %s with args: %s" query args)
  (try
    (log/spyf "Query Results: %s" (apply query db-spec args))
    (catch Exception e (log/error e "Query failed for args:" args))))

(defn save-track [db track]
  (log-query (:db-spec db) #'insert-track<! [(:post-id track)
                                             (:time track)
                                             (:media-url track)
                                             (:artist track)
                                             (:track track)
                                             (:parse-failed? track)]))

(defn add-spotify-uri [db track uri]
  (log-query (:db-spec db) #'update-spotify-uri! [uri, (:post-id track)]))

(defn track-exists? [db post-id]
  (:exists (first (log-query (:db-spec db) #'track-exists [post-id]))))

(defrecord Database [db-spec]
  component/Lifecycle

  (start [this]
    (log/info "Starting database")
    (log-query db-spec #'create-tracks! [])
    (assoc this :db-spec db-spec))

  (stop [this]
    (log/info "Stopping database")
    (assoc this :db-spec nil)))

(defn new-database [db-spec]
  (map->Database {:db-spec db-spec}))