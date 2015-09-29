(ns music-scraper.spotify.client
  (:require [environ.core :refer [env]]
            [clj-http.client :as client]
            [clojure.data.json :as json]))

(def access-token (atom nil))

(def spotify-creds
  {:client-id     (env :spotify-client-id)
   :client-secret (env :spotify-client-secret)})

(defn authenticate [client-id client-secret]
  (reset! access-token
          (:access_token
            (json/read-str (:body
                             (client/post "https://accounts.spotify.com/api/token"
                                          {:form-params {:grant_type "client_credentials"}
                                           :accept      :json
                                           :basic-auth  [(:client-id spotify-creds) (:client-secret spotify-creds)]}))
                           :key-fn keyword))))

(defn search-spotify-track [track]
  (json/read-str (:body (client/get "https://api.spotify.com/v1/search"
                                    {:query-params {:q track :type "track"}
                                     :accept       :json
                                     :oauth-token  @access-token}))))