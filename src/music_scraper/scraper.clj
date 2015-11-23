(ns music-scraper.scraper
  (:require [music-scraper.reddit.client :as reddit]
            [music-scraper.reddit.parse :as parse]
            [music-scraper.spotify.client :as spotify]
            [music-scraper.database :as database]
            [clojure.tools.logging :as log]
            [clj-http.client :as client]
            [com.stuartsierra.component :as component]))

(defn add-match [scraper match track]
  (reset! (:matched-tracks scraper) (conj @(:matched-tracks scraper) (:uri match)))
  (database/add-spotify-uri (:database scraper) track (:uri match)))

(defn process-result [scraper result]
  (database/save-track (:database scraper) result)
  (if (not (:parse-failed? result))
    (let [first-match (get (:items (:tracks (spotify/search-spotify-track (:spotify scraper) (:track result)))) 0)]
      (if (not (empty? (spotify/match-artist (:artist result) first-match)))
        (add-match scraper first-match result)))))

(defn process-page [scraper page]
  (let [filtered-results (filter #(not (database/track-exists? (:database scraper) (:id (:data %)))) (:children page))]
    (doseq [x (map #'parse/map-post filtered-results)]
      (process-result scraper x))))

(defn process
  ([scraper page]
   (process-page scraper page)
   (Thread/sleep 500)
   (if (not (nil? (:after page)))
     (process scraper (reddit/get-page reddit/url (:after page)))))
  ([scraper]
   (log/info "Processing results")
   (process scraper (reddit/get-page-data
                      (:body (client/get reddit/url {:accept        :json
                                                     :client-params {"http.useragent" "music-scraper"}}))))))

(defrecord Scraper [database spotify]
  component/Lifecycle

  (start [this]
    (log/info "Starting scraper")
    (assoc this :matched-tracks (atom [])))

  (stop [this]
    (log/info "Stopping scraper")
    (spotify/add-to-playlist (:spotify this) @(:matched-tracks this))))

(defn new-scraper [] (map->Scraper {}))