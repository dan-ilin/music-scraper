(ns music-scraper.database-test
  (:require [clojure.test :refer :all]
            [environ.core :refer [env]]
            [music-scraper.database :refer :all]
            [com.stuartsierra.component :as component]))

(deftest starts?
  (testing "starts?"
    (let [database (new-database {:classname   "org.postgresql.Driver"
                                  :subprotocol "postgresql"
                                  :subname     (env :database-url)
                                  :user        (env :database-user)
                                  :password    (env :database-pass)})]
      (is (not (nil? (:db-spec (component/start database))))))))
