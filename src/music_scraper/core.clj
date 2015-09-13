(ns music-scraper.core
  [:require [clj-http.client :as client]
            [clojure.data.json :as json]
            [clojure.string :as str]])

(def patterns {:artist-track #".* --? [^\[]*"
               :year         #"\(\d{4}\)"
               :genre-tags   #"\[.*\]"
               :comment      #"[^)]*$"})

(def url "https://www.reddit.com/r/listentothis/new.json")

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

(defn get-page-data [body]
  (:data (json/read-str body :key-fn keyword)))

(defn result-not-in-map [result]
  (let [post-id (:name result)]
    (contains? @result-map post-id)))

(def page (get-page-data (:body (client/get url {:accept :json :client-params {"http.useragent" "music-scraper"}}))))

(defn process-page [page]
  (doseq [x (map #'map-post (:children page))]
    (print x)
    (reset! result-map (assoc @result-map (:post-id x) x))))

(defn read-results []
  (json/read-str (slurp "results.json")))

(defn save-results [result-map]
  (spit "results.json" (json/write-str result-map)))