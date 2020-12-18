(defproject my-sec-home "0.1.0"
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [ring "1.7.1"]
                 [mount "0.1.16"]
                 [org.clojure/java.jdbc "0.7.11"]
                 [mysql/mysql-connector-java "8.0.20"]
                 [compojure "1.6.1"]
                                        ;[http-kit "2.3.0"]
                 [http-kit "2.4.0-alpha1"]
                 [ring/ring-defaults "0.3.2"]
                 [ring/ring-codec "1.1.2"]
                 [ring-oauth2 "0.1.5"]
                 [org.clojure/data.json "0.2.7"]
                 [ring/ring-json "0.4.0"]
                 [clj-time "0.15.2"]
                 [org.clojure/core.async "1.1.587"]
                 [clj.qrgen "0.4.0"]
                 [org.craigandera/dynne "0.4.1"]
                 #_[google-apps-clj "0.6.1"]]
  :main my-sec-home.core)