(ns music-scraper.core
  [:require [clojure.tools.logging :as log]
            [clj-http.client :as client]
            [clojure.string :as str]
            [clojure.java.io :as io]
            [music-scraper.store :as store]
            [music-scraper.reddit.client :as reddit]
            [music-scraper.spotify.client :as spotify]])

(def patterns {:artist-track #".* --? [^\[]*"
               :year         #"\(\d{4}\)"
               :genre-tags   #"\[.*\]"
               :comment      #"[^)]*$"})

(def url "https://www.reddit.com/r/listentothis/new.json")
(def filename "results.json")

(def matched-tracks (atom []))

(defn parse-track-data [track]
  (try
    (let [artist-track (str/split (re-find (:artist-track patterns) track) #" --? ")]
      {:artist     (str/trim (get artist-track 0))
       :track      (str/trim (get artist-track 1))
       :year       (subs (re-find (:year patterns) track) 1 5)
       :genre-tags (let [tags (re-find (:genre-tags patterns) track)]
                     (str/split (subs tags 1 (- (.length tags) 1)) #" "))
       :comment    (re-find (:comment patterns) track)})
    (catch Exception e
      (log/error e (format "Exception caught during parsing %s" track)))))

(defn map-post [post]
  (let [{data :data} post]
    (let [parsed-data (parse-track-data (:title data))]
      {:post-id       (:id data)
       :time          (:created_utc data)
       :media-url     (:url data)
       :artist        (:artist parsed-data)
       :track         (:track parsed-data)
       :parse-failed? (or (nil? (:artist parsed-data)) (nil? (:track parsed-data)))})))

(defn process-result [result]
  (if (not (:parse-failed? result))
    (let [first-match (get (:items (:tracks (spotify/search-spotify-track (:track result)))) 0)]
      (if (not (empty? (spotify/match-artist (:artist result) first-match)))
        (reset! matched-tracks (conj @matched-tracks (:uri first-match))))
      (reset! store/result-map (assoc @store/result-map (:post-id result) result)))))

(defn process-page [page]
  (let [filtered-results (filter #(not (contains? @store/result-map (:id (:data %)))) (:children page))]
    (doseq [x (map #'map-post filtered-results)]
      (process-result x))))

(defn process [page]
  (process-page page)
  (store/save-results filename @store/result-map)
  (log/info "Saving results to file")
  (Thread/sleep 500)
  (if (not (nil? (:after page)))
    (process (reddit/get-page url (:after page)))))

(defn -main [& args]
  (log/info "Loading previous results")
  (if (.exists (io/file filename))
    (reset! store/result-map (store/read-results filename))
    (log/info "No previous results found"))
  (log/info "Refreshing Spotify access token")
  (spotify/refresh-token (:client-id spotify/credentials)
                         (:client-secret spotify/credentials)
                         (:refresh-token spotify/credentials))
  (log/info "Processing results")
  (process (reddit/get-page-data (:body (client/get url {:accept        :json
                                                         :client-params {"http.useragent" "music-scraper"}}))))
  (log/info (format "Adding %d new tracks to Spotify playlist" (count @matched-tracks)))
  (spotify/add-to-playlist (:user-id spotify/credentials) (:playlist-id spotify/credentials) @matched-tracks))
