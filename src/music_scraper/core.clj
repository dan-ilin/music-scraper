(ns music-scraper.core
  (:gen-class :main true)
  [:require [clojure.tools.logging :as log]
            [clj-http.client :as client]
            [music-scraper.store :as store]
            [music-scraper.reddit.client :as reddit]
            [music-scraper.spotify.client :as spotify]
            [music-scraper.scraper :as scraper]])

(defn -main [& args]
  (store/setup)
  (log/info "Refreshing Spotify access token")
  (spotify/refresh-token (:client-id spotify/credentials)
                         (:client-secret spotify/credentials)
                         (:refresh-token spotify/credentials))
  (log/info "Processing results")
  (scraper/process (reddit/get-page-data (:body (client/get reddit/url {:accept        :json
                                                                        :client-params {"http.useragent" "music-scraper"}}))))
  (log/info (format "Adding %d new tracks to Spotify playlist" (count @scraper/matched-tracks)))
  (spotify/add-to-playlist @scraper/matched-tracks))