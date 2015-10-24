(ns music-scraper.store
  (:require [clojure.data.json :as json]
            [music-scraper.reddit.parse :as reddit]))

(def filename "results.json")

; store results and parsing failures in map by post-id
(def result-map (atom {}))

(defn read-results [filename]
  (json/read-str (slurp filename)))

(defn save-results [filename result-map]
  (spit filename (json/write-str result-map)))

(defn map-post [post]
  (let [{data :data} post]
    (let [parsed-data (reddit/parse-track-data (:title data))]
      {:post-id       (:id data)
       :time          (:created_utc data)
       :media-url     (:url data)
       :artist        (:artist parsed-data)
       :track         (:track parsed-data)
       :parse-failed? (or (nil? (:artist parsed-data)) (nil? (:track parsed-data)))})))
