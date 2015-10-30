(ns music-scraper.core
  (:gen-class :main true)
  [:require [environ.core :refer [env]]
            [music-scraper.database :refer [new-database]]
            [music-scraper.spotify.client :refer [new-client]]
            [music-scraper.scraper :refer [new-scraper]]
            [com.stuartsierra.component :as component]])

(defn scraper-system [config-options]
  (let [{:keys [database-url
                database-user
                database-pass
                spotify-client-id
                spotify-client-secret
                refresh-token
                user-id
                playlist-id]} config-options]
    (component/system-map
      :database (new-database database-url database-user database-pass)
      :spotify (new-client spotify-client-id spotify-client-secret refresh-token user-id playlist-id)
      :scraper (component/using
                 (new-scraper) [:database :spotify]))))

(def system (scraper-system {:database-url          (env :database-url)
                             :database-user         (env :database-user)
                             :database-pass         (env :database-pass)
                             :spotify-client-id     (env :spotify-client-id)
                             :spotify-client-secret (env :spotify-client-secret)
                             :refresh-token         (env :refresh-token)
                             :user-id               (env :user-id)
                             :playlist-id           (env :playlist-id)}))

(defn -main [& args]
  (component/start system)
  (component/stop system))