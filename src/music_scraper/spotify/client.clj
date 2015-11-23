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
  ([client tracks url]
   (let [resp (json/read-str
                (:body (client/get url {:query-params {:fields "next,items(track(uri))"}
                                        :oauth-token  (:access-token client)})))
         items (concat tracks (map #(get (get % "track") "uri") (get resp "items")))
         next (get resp "next")]
     (if (not (nil? next))
       (get-playlist-tracks client items next)
       items)))
  ([client]
   (get-playlist-tracks client
                        []
                        (format "https://api.spotify.com/v1/users/%s/playlists/%s/tracks"
                                (:user-id client) (:playlist-id client)))))

(defn add-to-playlist [client tracks]
  (let [filtered-tracks (filter #(not (contains? @(:playlist-tracks client) %)) tracks)]
    (log/infof "Adding %d new tracks to playlist %s" (count filtered-tracks) (:playlist-id client))
    (doseq [x (partition 100 100 nil filtered-tracks)]
      (client/post (format "https://api.spotify.com/v1/users/%s/playlists/%s/tracks"
                           (:user-id client)
                           (:playlist-id client))
                   {:form-params  {:uris x}
                    :content-type :json
                    :oauth-token  (:access-token client)})
      (reset! (:playlist-tracks client) (concat @(:playlist-tracks client) x)))))

(defn match-artist [artist result]
  (filter (fn [y] (.equalsIgnoreCase artist (:name y))) (:artists result)))

(defrecord Client [client-id client-secret token user-id playlist-id]
  component/Lifecycle

  (start [this]
    (log/info "Starting Spotify client")
    (let [resp (refresh-token client-id client-secret token)
          client (assoc this :client-id client-id
                             :client-secret client-secret
                             :access-token (:access_token resp)
                             :user-id user-id
                             :playlist-id playlist-id)]
      (assoc client :playlist-tracks (atom (set (get-playlist-tracks client))))))

  (stop [this]
    (log/info "Stopping Spotify client")
    (assoc this :access-token nil
                :playlist-tracks nil)))

(defn new-client [client-id client-secret refresh-token user-id playlist-id]
  (map->Client {:client-id     client-id
                :client-secret client-secret
                :token         refresh-token
                :user-id       user-id
                :playlist-id   playlist-id}))