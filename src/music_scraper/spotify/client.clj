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
  ([client] (refresh-token (:client-id client) (:client-secret client) (:refresh-token client))))

(defrecord client [client-id client-secret refresh-token user-id playlist-id]
  component/Lifecycle

  (start [component]
    (let [resp (refresh-token client-id client-secret refresh-token)]
      (assoc component :client-id client-id
                       :client-secret client-secret
                       :access-token (:access-token resp)
                       :refresh-token (:refresh-token resp)
                       :user-id user-id
                       :playlist-id playlist-id)))

  (stop [component]
    (assoc component :access-token nil)))

(defn search-spotify-track [client track]
  (log/infof "Searching Spotify for %s" track)
  (Thread/sleep 25)
  (json/read-str (:body (client/get "https://api.spotify.com/v1/search"
                                    {:query-params {:q track :type "track"}
                                     :accept       :json
                                     :oauth-token  (:access-token client)}))
                 :key-fn keyword))

(defn add-to-playlist [client tracks]
  (log/infof "Adding %d new tracks to Spotify playlist" (count tracks))
  (if (not (empty? tracks))
    (doseq [x (partition 10 tracks)]
      (Thread/sleep 100)
      (client/post (format "https://api.spotify.com/v1/users/%s/playlists/%s/tracks"
                           (:user-id client)
                           (:playlist-id client))
                   {:query-params {:uris (join "," x)}
                    :oauth-token  (:access-token client)}))))

(defn match-artist [artist result]
  (filter (fn [y] (.equalsIgnoreCase artist (:name y))) (:artists result)))
