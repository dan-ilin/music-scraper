(ns music-scraper.core-test
  (:require [clojure.test :refer :all]
            [music-scraper.core :refer :all]
            [environ.core :refer [env]]
            [com.stuartsierra.component :as component]))

(deftest system-starts?
  (testing "system-starts?"
    (let [system (component/start (scraper-system {:database-url          (env :database-url)
                                                   :database-user         (env :database-user)
                                                   :database-pass         (env :database-pass)
                                                   :spotify-client-id     (env :spotify-client-id)
                                                   :spotify-client-secret (env :spotify-client-secret)
                                                   :refresh-token         (env :refresh-token)
                                                   :user-id               (env :user-id)
                                                   :playlist-id           (env :playlist-id)}))]
      (is (and (not (nil? (:database system)))
               (not (nil? (:spotify system)))
               (not (nil? (:scraper system)))
               (not (nil? (:spotify (:scraper system))))
               (not (nil? (:database (:scraper system)))))))))
