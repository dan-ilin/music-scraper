(ns music-scraper.spotify.client
  (:require [environ.core :refer [env]]
            [clj-http.client :as client]
            [clojure.data.json :as json]
            [clojure.string :refer [join]]
            [clojure.tools.logging :as log]))

(def access-token (atom (env :access-token)))

(def credentials {:client-id     (env :spotify-client-id)
                  :client-secret (env :spotify-client-secret)
                  :refresh-token (env :refresh-token)
                  :user-id       (env :user-id)
                  :playlist-id   (env :playlist-id)})

(defn refresh-token [client-id client-secret refresh-token]
  (log/info "Refreshing Spotify access token")
  (reset! access-token
          (:access_token (json/read-str (:body (client/post "https://accounts.spotify.com/api/token"
                                                            {:form-params {:grant_type    "refresh_token"
                                                                           :refresh_token refresh-token}
                                                             :accept      :json
                                                             :basic-auth  [client-id client-secret]}))
                                        :key-fn keyword))))

(defn search-spotify-track [track]
  (log/infof "Searching Spotify for %s" track)
  (json/read-str (:body (client/get "https://api.spotify.com/v1/search"
                                    {:query-params {:q track :type "track"}
                                     :accept       :json
                                     :oauth-token  @access-token}))
                 :key-fn keyword))

(defn add-to-playlist [tracks]
  (log/infof "Adding %d new tracks to Spotify playlist" (count tracks))
  (if (not (empty? tracks))
    (doseq [x (partition 10 tracks)]
      (Thread/sleep 100)
      (client/post (format "https://api.spotify.com/v1/users/%s/playlists/%s/tracks"
                           (:user-id credentials)
                           (:playlist-id credentials))
                   {:query-params {:uris (join "," x)}
                    :oauth-token  @access-token}))))

(defn match-artist [artist result]
  (filter (fn [y] (.equalsIgnoreCase artist (:name y))) (:artists result)))

(defn start []
  (refresh-token (:client-id credentials)
                 (:client-secret credentials)
                 (:refresh-token credentials)))