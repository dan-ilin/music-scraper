(ns music-scraper.database
  (:require [clojure.tools.logging :as log]
            [environ.core :refer [env]]
            [music-scraper.reddit.parse :as reddit]
            [yesql.core :refer [defqueries]]
            [com.stuartsierra.component :as component]))

(defn log-query [component query args]
  (log/infof "Running Query %s with args: %s" query args)
  (try
    (log/spyf "Query Results: %s" (apply query (:db-spec component) args))
    (catch Exception e (log/error e "Query failed for args:" args))))

(defn save-track [component track]
  (log-query component #'insert-track<! [(:post-id track)
                                         (:time track)
                                         (:media-url track)
                                         (:artist track)
                                         (:track track)]))

(defn add-spotify-uri [component track uri]
  (log-query component #'update-spotify-uri! [uri, (:post-id track)]))

(defn track-exists? [component post-id]
  (:exists (log-query component #'track-exists [post-id])))

(defrecord Database [url user pass]
  ;; Implement the Lifecycle protocol
  component/Lifecycle

  (start [component]
    (log/info "Setting up database")
    (defqueries "sql/tracks.sql")
    (let [db-spec {:classname   "org.postgresql.Driver"
                   :subprotocol "postgresql"
                   :subname     url
                   :user        user
                   :password    pass}]
      (create-tracks! db-spec nil)
      (assoc component :db-spec db-spec)))

  (stop [component]
    (assoc component :db-spec nil)))

(defn new-database [url user pass]
  (map->Database {:url url :user user :pass pass}))