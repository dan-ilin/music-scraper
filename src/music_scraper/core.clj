(ns music-scraper.core
  (:gen-class :main true)
  [:require [clojure.tools.logging :as log]
            [clj-http.client :as client]
            [clojure.java.io :as io]
            [music-scraper.store :as store]
            [music-scraper.reddit.client :as reddit]
            [music-scraper.spotify.client :as spotify]])

(def matched-tracks (atom []))

(defn process-result [result]
  (if (not (:parse-failed? result))
    (let [first-match (get (:items (:tracks (spotify/search-spotify-track (:track result)))) 0)]
      (if (not (empty? (spotify/match-artist (:artist result) first-match)))
        (reset! matched-tracks (conj @matched-tracks (:uri first-match))))
      (reset! store/result-map (assoc @store/result-map (:post-id result) result)))))

(defn process-page [page]
  (let [filtered-results (filter #(not (contains? @store/result-map (:id (:data %)))) (:children page))]
    (doseq [x (map #'store/map-post filtered-results)]
      (process-result x))))

(defn process [page]
  (process-page page)
  (store/save-results store/filename @store/result-map)
  (log/info "Saving results to file")
  (Thread/sleep 500)
  (if (not (nil? (:after page)))
    (process (reddit/get-page reddit/url (:after page)))))

(defn -main [& args]
  (log/info "Loading previous results")
  (if (.exists (io/file store/filename))
    (reset! store/result-map (store/read-results store/filename))
    (log/info "No previous results found"))
  (log/info "Refreshing Spotify access token")
  (spotify/refresh-token (:client-id spotify/credentials)
                         (:client-secret spotify/credentials)
                         (:refresh-token spotify/credentials))
  (log/info "Processing results")
  (process (reddit/get-page-data (:body (client/get reddit/url {:accept        :json
                                                                :client-params {"http.useragent" "music-scraper"}}))))
  (log/info (format "Adding %d new tracks to Spotify playlist" (count @matched-tracks)))
  (spotify/add-to-playlist @matched-tracks))
