(ns music-scraper.core
  (:gen-class :main true)
  [:require [music-scraper.database :as database]
            [music-scraper.spotify.client :as spotify]
            [music-scraper.scraper :as scraper]])

(defn -main [& args]
  (database/start)
  (spotify/start)
  (scraper/start))