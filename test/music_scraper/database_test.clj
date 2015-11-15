(ns music-scraper.database-test
  (:require [clojure.test :refer :all]
            [environ.core :refer [env]]
            [music-scraper.database :refer :all]
            [com.stuartsierra.component :as component]))

(deftest starts?
  (testing "starts?"
    (let [database (new-database (env :db-spec))]
      (is (not (nil? (:db-spec (component/start database))))))))
