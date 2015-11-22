(ns music-scraper.reddit.parse
  (:require [clojure.tools.logging :as log]
            [clojure.string :as str])
  (:import (java.sql Timestamp)))

(def patterns {:artist-track #".* --? [^\[]*"
               :year         #"\(\d{4}\)"
               :genre-tags   #"\[.*\]"
               :comment      #"[^)]*$"})

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
  (let [{data :data} post
        parsed-data (parse-track-data (:title data))]
    {:post-id       (:id data)
     :time          (new Timestamp (* 1000 (:created_utc data))) ;; convert epoch timestamp to java.sql.Timestamp
     :media-url     (:url data)
     :artist        (:artist parsed-data)
     :track         (:track parsed-data)
     :parse-failed? (or (nil? (:artist parsed-data)) (nil? (:track parsed-data)))}))
