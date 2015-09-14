(ns music-scraper.core
  [:require [clj-http.client :as client]
            [clojure.data.json :as json]
            [clojure.string :as str]
            [clojure.java.io :as io]])

(def patterns {:artist-track #".* --? [^\[]*"
               :year         #"\(\d{4}\)"
               :genre-tags   #"\[.*\]"
               :comment      #"[^)]*$"})

(def url "https://www.reddit.com/r/listentothis/new.json")
(def filename "results.json")

; store results in map by post-id
(def result-map (atom {}))

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
      (println e))))

(defn map-post [post]
  (let [{data :data} post]
    (let [parsed-data (parse-track-data (:title data))]
      {:post-id       (:name data)
       :time          (:created_utc data)
       :media-url     (:url data)
       :artist        (:artist parsed-data)
       :track         (:track parsed-data)
       :parse-failed? (or (nil? (:artist parsed-data)) (nil? (:track parsed-data)))})))

(defn result-not-in-map [map result]
  (not (contains? map (:name (:data result)))))

(defn get-page-data [body]
  (:data (json/read-str body :key-fn keyword)))

(def page (get-page-data (:body (client/get url {:accept :json :client-params {"http.useragent" "music-scraper"}}))))

(defn process-page [page]
  (let [filtered-results (filter (partial result-not-in-map @result-map) (:children page))]
    (println (count filtered-results))
    (doseq [x (map #'map-post filtered-results)]
      (reset! result-map (assoc @result-map (:post-id x) x)))))

(defn read-results [filename]
  (json/read-str (slurp filename)))

(defn save-results [filename result-map]
  (spit filename (json/write-str result-map)))

(defn -main [& args]
  (println "Loading previous results")
  (if (.exists (io/file filename))
    (reset! result-map (read-results filename))
    (println "No previous results found"))
  (println "Processing results")
  (process-page page)
  (println "Saving results")
  (save-results filename @result-map))