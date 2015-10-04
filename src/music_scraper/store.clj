(ns music-scraper.store
  (:require [clojure.data.json :as json]))

; store results and parsing failures in map by post-id
(def result-map (atom {}))

(defn read-results [filename]
  (json/read-str (slurp filename)))

(defn save-results [filename result-map]
  (spit filename (json/write-str result-map)))
