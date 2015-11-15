(ns music-scraper.core
  (:gen-class :main true)
  (:require [environ.core :refer [env]]
            [music-scraper.database :refer [new-database]]
            [music-scraper.spotify.client :refer [new-client]]
            [music-scraper.scraper :refer [new-scraper process]]
            [com.stuartsierra.component :as component]))

(defn new-system [config-options]
  (let [{:keys [db-spec
                spotify-client-id
                spotify-client-secret
                refresh-token
                user-id
                playlist-id]} config-options]
    (-> (component/system-map
          :database (new-database db-spec)
          :spotify (new-client spotify-client-id spotify-client-secret refresh-token user-id playlist-id)
          :scraper (new-scraper))
        (component/system-using {:scraper [:database :spotify]}))))

(defn -main [& args]
  (let [system (component/start
                 (new-system {:db-spec               (env :db-spec)
                              :spotify-client-id     (env :spotify-client-id)
                              :spotify-client-secret (env :spotify-client-secret)
                              :refresh-token         (env :refresh-token)
                              :user-id               (env :user-id)
                              :playlist-id           (env :playlist-id)}))]
    (process (:scraper system))
    (component/stop system)))