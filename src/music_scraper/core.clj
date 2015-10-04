(ns music-scraper.core
  [:require [clj-http.client :as client]
            [clojure.data.json :as json]
            [clojure.string :as str]
            [clojure.java.io :as io]
            [music-scraper.store :as store]
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

(defn get-page-data [body]
  (:data (json/read-str body :key-fn keyword)))

(defn get-page [base-url after]
  (get-page-data (:body
                   (client/get base-url
                               {:query-params  {:after after}
                                :accept        :json
                                :client-params {"http.useragent" "music-scraper"}}))))

(defn process [page]
  (let [filtered-results (filter (partial result-not-in-map @store/result-map) (:children page))]
    (doseq [x (map #'map-post filtered-results)]
      (reset! store/result-map (assoc @store/result-map
                                 (:post-id x) x))))
  (store/save-results filename @store/result-map)
  (println "Saving results to file")
  (Thread/sleep 500)
  (if (not (nil? (:after page)))
    (process (get-page url (:after page)))))

(defn -main [& args]
  (let [page (get-page-data
               (:body (client/get url {:accept :json :client-params {"http.useragent" "music-scraper"}})))]
    (println "Loading previous results")
    (if (.exists (io/file filename))
      (reset! store/result-map (store/read-results filename))
      (println "No previous results found"))
    (println "Processing results")
    (process page)))
