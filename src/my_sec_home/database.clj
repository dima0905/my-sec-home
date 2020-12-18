;;
(ns my-sec-home.database
  (:require [clojure.java.jdbc :as jdbc]))
;
(def beasts-db-spec {:dbtype "mysql" :subname "Intel@localhost" :dbname "my_sec_home" :user "root" :password "Vorona0905"})
;
(def _beasts (atom {})) (def _owners (atom {}))
;
(def honeypot (atom {})) 
;
(doseq [{:keys [mac owner alias]} (jdbc/query my-sec-home.database/beasts-db-spec ["select * from beasts;"])]
  (swap! _beasts conj {mac {:owner owner :pass (atom nil)}})
  (when (not (contains? @_owners owner)) (swap! _owners conj {owner {:zoo (atom {}) :active (atom nil) :gateway (atom nil)}}))
  (swap! (:zoo (@_owners owner)) conj {mac {:acceptor (atom ""#_(intern 'my-sec-home.core (gensym))) :alias alias :status (atom :in-bed) :mode (atom :top-kb) :post-pic (atom nil)}}))
;
(defn recording-to-base [mac owner alias]
  (jdbc/insert! my-sec-home.database/beasts-db-spec :beasts {:mac mac :owner owner :alias alias}))
;
(defn deleting-from-base [mac] (jdbc/delete! my-sec-home.database/beasts-db-spec :beasts ["mac=?" mac]))
;
(defn from-base-by-mac [mac] (jdbc/query my-sec-home.database/beasts-db-spec ["select * from beasts where mac = ?" mac]))
;
(defn update-to-base [mac key val] (jdbc/update! my-sec-home.database/beasts-db-spec :beasts {key val} ["mac=?" mac]))   
