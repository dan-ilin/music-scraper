(ns music-scraper.core
  [:require [clj-http.client :as client]
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

(defn parse-track-data [track]
  (try
    (let [artist-track (str/split (re-find (:artist-track patterns) track) #" --? ")]
      {:artist     (str/trim (get artist-track 0))
       :track      (str/trim (get artist-track 1))
       :year       (subs (re-find (:year patterns) track) 1 5)
       :genre-tags (let [tags (re-find (:genre-tags patterns) track)]
                     (str/split (subs tags 1 (- (.length tags) 1)) #" "))
       :comment    (re-find (:comment patterns) track)})
    (catch Exception e)))

(defn map-post [post]
  (let [{data :data} post]
    (let [parsed-data (parse-track-data (:title data))]
      {:post-id       (:id data)
       :time          (:created_utc data)
       :media-url     (:url data)
       :artist        (:artist parsed-data)
       :track         (:track parsed-data)
       :parse-failed? (or (nil? (:artist parsed-data)) (nil? (:track parsed-data)))})))

(defn result-not-in-map [map result]
  (not (contains? map (:id (:data result)))))

(defn process [page]
  (let [filtered-results (filter (partial result-not-in-map @store/result-map) (:children page))]
    (doseq [x (map #'map-post filtered-results)]
      (if (not (:parse-failed? x))
        (let [first-match (get (:items (:tracks (spotify/search-spotify-track (:track x)))) 0)]
          (if (not (empty? (spotify/match-artist (:artist x) first-match)))
            (reset! store/result-map (assoc @store/result-map (:post-id x) (assoc x :spotify-match first-match)))
            (reset! store/result-map (assoc @store/result-map (:post-id x) x)))))))
  (store/save-results filename @store/result-map)
  (println "Saving results to file")
  (Thread/sleep 500)
  (if (not (nil? (:after page)))
    (process (reddit/get-page url (:after page)))))

(defn -main [& args]
  (let [page (reddit/get-page-data
               (:body (client/get url {:accept :json :client-params {"http.useragent" "music-scraper"}})))]
    (println "Loading previous results")
    (if (.exists (io/file filename))
      (reset! store/result-map (store/read-results filename))
      (println "No previous results found"))
    (println "Processing results")
    (process page)))
