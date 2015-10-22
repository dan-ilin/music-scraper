(ns music-scraper.reddit.client
  (:require [clj-http.client :as client]
            [clojure.data.json :as json]))

(def url "https://www.reddit.com/r/listentothis/new.json")

(defn get-page-data [body]
  (:data (json/read-str body :key-fn keyword)))

(defn get-page [base-url after]
  (get-page-data (:body
                   (client/get base-url
                               {:query-params  {:after after}
                                :accept        :json
                                :client-params {"http.useragent" "music-scraper"}}))))