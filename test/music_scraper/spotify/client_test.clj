(ns music-scraper.spotify.client-test
  (:require [clojure.test :refer :all]
            [environ.core :refer [env]]
            [music-scraper.spotify.client :refer :all]
            [com.stuartsierra.component :as component]
            [clojure.tools.logging :as log]))

(deftest starts?
  (testing "starts?"
    (let [client (new-client (env :spotify-client-id)
                             (env :spotify-client-secret)
                             (env :refresh-token)
                             (env :user-id)
                             (env :playlist-id))]
      (is (not (nil? (:access-token (component/start client))))))))
