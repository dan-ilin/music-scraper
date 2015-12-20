(ns music-scraper.scraper
  (:require [music-scraper.reddit.client :as reddit]
            [music-scraper.reddit.parse :as parse]
            [music-scraper.spotify.client :as spotify]
            [music-scraper.database :as database]
            [clojure.tools.logging :as log]
            [clj-http.client :as client]
            [com.stuartsierra.component :as component]
            [clojure.core.async
             :as a
             :refer [>! <! >!! <!! go chan buffer close! thread
                     alts! alts!! timeout]]))

(defn process
  ([scraper page]
   (>!! (:in-chan scraper) (:children page))
   (Thread/sleep 500)
   (if (not (nil? (:after page)))
     (process scraper (reddit/get-page reddit/url (:after page)))))
  ([scraper]
   (log/info "Processing results")
   (process scraper
            (reddit/get-page-data (:body (client/get reddit/url
                                                     {:accept        :json
                                                      :client-params {"http.useragent" "music-scraper"}}))))))

(defn page-processor [in scraper]
  (let [out (chan)]
    (go (while true
          (let [x (filter #(not (database/track-exists? (:database scraper) (:id (:data %)))) (<! in))]
            (>! out x))))
    out))

(defn page-children-processor [in]
  (let [out (chan)]
    (go (while true
          (doseq [x (filter #(not (nil? %)) (map #'parse/map-post (<! in)))]
            (>! out x))))
    out))

(defn result-saver [in scraper]
  (let [out (chan)]
    (go (while true
          (let [x (<! in)]
            (database/save-track (:database scraper) x)
            (if (not (:parse-failed? x))
              (>! out x)))))
    out))

(defn result-searcher [in scraper]
  (let [out (chan 1000 (partition-all 100))]
    (go (while true
          (let [x (<! in)
                first-match (get (:items (:tracks (spotify/search-spotify-track (:spotify scraper) (:track x)))) 0)]
            (if (not (empty? (spotify/match-artist (:artist x) first-match)))
              (>! out (assoc x :uri (:uri first-match)))))))))

(defn spotify-processor [in scraper]
  (go (while true
        (let [x (<! in)]
          (database/add-spotify-uri (:database scraper) x)
          (spotify/add-to-playlist (:spotify scraper) (:uri x))))))

(defrecord Scraper [database spotify]
  component/Lifecycle

  (start [this]
    (log/info "Starting scraper")
    (let [in-chan (chan)
          page-processor (page-processor in-chan this)
          child-processor (page-children-processor page-processor)
          result-saver (result-saver child-processor this)
          result-searcher (result-searcher result-saver this)
          spotify-processor (spotify-processor result-searcher this)]
      (assoc this :in-chan in-chan
                  :out-chan spotify-processor)))

  (stop [this]
    (log/info "Stopping scraper")))

(defn new-scraper [] (map->Scraper {}))