(ns music-scraper.core
  [:require [clj-http.client :as client]
            [clojure.data.json :as json]
            [clojure.string :as str]])

(def patterns {:artist-track #".* --? [^\[]*"
               :year         #"\(\d{4}\)"
               :genre-tags   #"\[.*\]"
               :comment      #"[^)]*$"})

(defn parse-track-data [track]
  (let [artist-track (str/split (re-find (:artist-track patterns) track) #" --? ")]
    {:artist     (str/trim (get artist-track 0))
     :track      (str/trim (get artist-track 1))
     :year       (subs (re-find (:year patterns) track) 1 5)
     :genre-tags (let [tags (re-find (:genre-tags patterns) track)]
                   (str/split (subs tags 1 (- (.length tags) 1)) #" "))
     :comment    (re-find (:comment patterns) track)}))

(defn map-post [post]
  (try
    (let [{data :data} post]
      (let [parsed-data (parse-track-data (:title data))]
        {:post-id       (:name data)
         :time          (:created_utc data)
         :media-url     (:url data)
         :artist        parsed-data
         :track         parsed-data
         :parse-failed? (or (nil? (:artist parsed-data)) (nil? (:track parsed-data)))}))
    (catch Exception e
      (println e))))

(defn get-page-data [body]
  (:data (json/read-str body :key-fn keyword)))

(defn process-page [page]
  (map #'map-post (:childriin page)))