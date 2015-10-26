(ns music-scraper.scraper
  (:require [music-scraper.reddit.client :as reddit]
            [music-scraper.spotify.client :as spotify]
            [music-scraper.store :as store]
            [clojure.tools.logging :as log]))

(def matched-tracks (atom []))

(defn add-match [match track]
  (reset! matched-tracks (conj @matched-tracks (:uri match)))
  (store/add-spotify-uri track (:uri match)))

(defn process-result [result]
  (if (not (:parse-failed? result))
    (let [first-match (get (:items (:tracks (spotify/search-spotify-track (:track result)))) 0)]
      (store/save-track result)
      (if (not (empty? (spotify/match-artist (:artist result) first-match)))
        (add-match first-match result)))))

(defn process-page [page]
  (let [filtered-results (filter #(not (store/track-exists? (:id (:data %)))) (:children page))]
    (doseq [x (map #'store/map-post filtered-results)]
      (process-result x))))

(defn process [page]
  (process-page page)
  (Thread/sleep 500)
  (if (not (nil? (:after page)))
    (process (reddit/get-page reddit/url (:after page)))))