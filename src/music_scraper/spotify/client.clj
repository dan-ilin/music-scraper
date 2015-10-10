(ns music-scraper.spotify.client
  (:require [environ.core :refer [env]]
            [clj-http.client :as client]
            [clojure.data.json :as json]))

(def access-token (atom nil))
(def account-auth (atom nil))

(def spotify-creds
  {:client-id     (env :spotify-client-id)
   :client-secret (env :spotify-client-secret)
   :code          (env :spotify-auth-code)
   :redirect-uri  (env :redirect-uri)})

(defn authenticate
  ([client-id client-secret]
   (reset! access-token
           (:access_token (json/read-str (:body
                                           (client/post "https://accounts.spotify.com/api/token"
                                                        {:form-params {:grant_type "client_credentials"}
                                                         :accept      :json
                                                         :basic-auth  [(:client-id spotify-creds)
                                                                       (:client-secret spotify-creds)]}))
                                         :key-fn keyword))))
  ([client-id client-secret code]
   (reset! account-auth
           (json/read-str (:body (client/post "https://accounts.spotify.com/api/token"
                                              {:form-params {:grant_type   "authorization_code"
                                                             :code         (:code spotify-creds)
                                                             :redirect_uri (:redirect-uri spotify-creds)}
                                               :accept      :json
                                               :basic-auth  [(:client-id spotify-creds)
                                                             (:client-secret spotify-creds)]}))))))

(defn refresh-token [client-id client-secret refresh-token]
  (reset! account-auth
          (json/read-str (:body (client/post "https://accounts.spotify.com/api/token"
                                             {:form-params {:grant_type    "refresh_token"
                                                            :refresh_token refresh-token}
                                              :accept      :json
                                              :basic-auth  [(:client-id spotify-creds)
                                                            (:client-secret spotify-creds)]})))))

(defn search-spotify-track [track]
  (json/read-str (:body (client/get "https://api.spotify.com/v1/search"
                                    {:query-params {:q track :type "track"}
                                     :accept       :json
                                     :oauth-token  @access-token}))
                 :key-fn keyword))

(defn match-artist [artist result]
  (filter (fn [y] (.equalsIgnoreCase artist (:name y))) (:artists result)))
