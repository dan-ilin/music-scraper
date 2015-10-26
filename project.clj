(defproject music-scraper "0.1.0-SNAPSHOT"
  :description "An application that scrapes new posts from Reddit's r/listentothis and adds them to a Spotify playlist"
  :url "https://github.com/dan-ilin/music-scraper"
  :license {:name "Eclipse Public License"
            :url  "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/tools.logging "0.3.1"]
                 [log4j/log4j "1.2.17" :exclusions [javax.mail/mail
                                                    javax.jms/jms
                                                    com.sun.jdmk/jmxtools
                                                    com.sun.jmx/jmxri]]
                 [org.clojure/data.json "0.2.6"]
                 [clj-http "2.0.0"]
                 [environ "1.0.1"]
                 [yesql "0.4.2"]
                 [postgresql "9.3-1102.jdbc41"]]
  :plugins [[lein-environ "1.0.1"]]
  :main music-scraper.core
  :aot [music-scraper.core])
