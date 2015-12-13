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
   (>!! (:in-chan scraper) page)
   (Thread/sleep 500)
   (if (not (nil? (:after page)))
     (process scraper (reddit/get-page reddit/url (:after page)))))
  ([scraper]
   (log/info "Processing results")
   (process scraper
            (reddit/get-page-data (:body (client/get reddit/url
                                                     {:accept        :json
                                                      :client-params {"http.useragent" "music-scraper"}}))))))

(defn add-match [scraper uri track]
  (log/info uri)
  (database/add-spotify-uri (:database scraper) track uri)
  uri)

(defn process-result [scraper result]
  (database/save-track (:database scraper) result)
  (if (not (:parse-failed? result))
    (let [first-match (get (:items (:tracks (spotify/search-spotify-track (:spotify scraper) (:track result)))) 0)]
      (if (not (empty? (spotify/match-artist (:artist result) first-match)))
        (add-match scraper (:uri first-match) result)))))   ; fixme: cannot return nil here, add an else condition to fix

(defn page-processor [in scraper]
  (let [out (chan)]
    (go (while true
          (let [x (filter #(not (database/track-exists? (:database scraper) (:id (:data %)))) (:children (<! in)))]
            (>! out x))))
    out))

(defn page-children-processor [in]
  (let [out (chan)]
    (go (while true
          (doseq [x (filter #(not (nil? %)) (map #'parse/map-post (<! in)))]
            (>! out x))))
    out))

(defn result-processor [in scraper]
  (let [out (chan 1000 (partition-all 100))]
    (go (while true
          (let [x (process-result scraper (<! in))]
            (if (not (nil? x))
              (>! out x)))))
    out))

(defn spotify-processor [in scraper]
  (go (while true
        (Thread/sleep 100)
        (spotify/add-to-playlist (:spotify scraper) (<! in)))))

(defrecord Scraper [database spotify]
  component/Lifecycle

  (start [this]
    (log/info "Starting scraper")
    (let [in-chan (chan)]
      (assoc this :in-chan in-chan
                  :out-chan (spotify-processor (result-processor (page-children-processor (page-processor in-chan this)) this) this))))

  (stop [this]
    (log/info "Stopping scraper")))

(defn new-scraper [] (map->Scraper {}))