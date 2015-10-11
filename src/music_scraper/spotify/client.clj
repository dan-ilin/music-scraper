(ns music-scraper.spotify.client
  (:require [environ.core :refer [env]]
            [clj-http.client :as client]
            [clojure.data.json :as json]))

(def access-token (atom (env :access-token)))

(def credentials {:client-id     (env :spotify-client-id)
                  :client-secret (env :spotify-client-secret)
                  :refresh-token (env :refresh-token)})

(defn refresh-token [client-id client-secret refresh-token]
  (reset! access-token
          (:access_token
            (json/read-str (:body
                             (client/post "https://accounts.spotify.com/api/token"
                                          {:form-params {:grant_type    "refresh_token"
                                                         :refresh_token refresh-token}
                                           :accept      :json
                                           :basic-auth  [client-id client-secret]}))
                           :key-fn keyword))))

(defn search-spotify-track [track]
  (json/read-str (:body (client/get "https://api.spotify.com/v1/search"
                                    {:query-params {:q track :type "track"}
                                     :accept       :json
                                     :oauth-token  @access-token}))
                 :key-fn keyword))

(defn match-artist [artist result]
  (filter (fn [y] (.equalsIgnoreCase artist (:name y))) (:artists result)))
