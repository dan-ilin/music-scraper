(ns music-scraper.spotify.client
  (:require [environ.core :refer [env]]
            [clj-http.client :as client]
            [clojure.data.json :as json]
            [clojure.string :refer [join]]
            [clojure.tools.logging :as log]
            [com.stuartsierra.component :as component]))

(defn refresh-token
  ([client-id client-secret refresh-token]
   (log/info "Refreshing Spotify access token")
   (json/read-str (:body (client/post "https://accounts.spotify.com/api/token"
                                      {:form-params {:grant_type    "refresh_token"
                                                     :refresh_token refresh-token}
                                       :accept      :json
                                       :basic-auth  [client-id client-secret]}))
                  :key-fn keyword))
  ([client] (refresh-token (:client-id client) (:client-secret client) (:token client))))

(defn search-spotify-track [client track]
  (log/infof "Searching Spotify for %s" track)
  (Thread/sleep 25)
  (json/read-str (:body (client/get "https://api.spotify.com/v1/search"
                                    {:query-params {:q track :type "track"}
                                     :accept       :json
                                     :oauth-token  (:access-token client)}))
                 :key-fn keyword))

(defn get-playlist-tracks
  ([client url]
   (json/read-str
     (:body (client/get url {:query-params {:fields "items(track(uri))"}
                             :oauth-token  (:access-token client)}))))
  ([client user-id playlist-id]
   (get-playlist-tracks client (format "https://api.spotify.com/v1/users/%s/playlists/%s/tracks" user-id playlist-id))))

(defn add-to-playlist [client tracks]
  (log/infof "Adding %d new tracks to Spotify playlist" (count tracks))
  (if (not (empty? tracks))
    (doseq [x (partition 100 tracks)]
      (Thread/sleep 100)
      (client/post (format "https://api.spotify.com/v1/users/%s/playlists/%s/tracks"
                           (:user-id client)
                           (:playlist-id client))
                   {:query-params {:uris (join "," x)}
                    :oauth-token  (:access-token client)}))))

(defn match-artist [artist result]
  (filter (fn [y] (.equalsIgnoreCase artist (:name y))) (:artists result)))


(defrecord Client [client-id client-secret token user-id playlist-id]
  component/Lifecycle

  (start [this]
    (log/info "Starting Spotify client")
    (let [resp (refresh-token client-id client-secret token)]
      (assoc this :client-id client-id
                  :client-secret client-secret
                  :access-token (:access_token resp)
                  :user-id user-id
                  :playlist-id playlist-id)))

  (stop [this]
    (log/info "Stopping Spotify client")
    (assoc this :access-token nil)))

(defn new-client [client-id client-secret refresh-token user-id playlist-id]
  (map->Client {:client-id     client-id
                :client-secret client-secret
                :token         refresh-token
                :user-id       user-id
                :playlist-id   playlist-id}))