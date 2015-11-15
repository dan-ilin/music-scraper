(ns music-scraper.scraper-test
  (:require [clojure.test :refer :all]
            [environ.core :refer [env]]
            [music-scraper.scraper :refer :all]
            [com.stuartsierra.component :as component]))

(deftest starts?
  (testing "starts?"
    (let [scraper (new-scraper)]
      (is (not (nil? (:matched-tracks (component/start scraper))))))))
