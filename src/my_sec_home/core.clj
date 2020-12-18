;;
(ns my-sec-home.core
  (:require [org.httpkit.server :as server]
            [org.httpkit.client :as http]
            [ring.adapter.jetty :as jetty]
            [mount.core :as mount :refer [defstate]]
            [compojure.core :refer :all]
            [compojure.route :as route ]
            [ring.middleware.defaults :refer :all]
            [ring.middleware.params :refer :all]
            [ring.middleware.multipart-params :refer :all]
            [ring.middleware.json :refer [wrap-json-body]]
            [ring.handler.dump :refer :all]
            [ring.util.codec :refer :all]
            [clojure.pprint :as pp]
            [clojure.string :as str]
            [clojure.data.json :as json]
            [clojure.java.shell :as shell]
            [clojure.java.io :as io]
            [clj-time.core :as t]
            [my-sec-home.database :refer [_owners _beasts honeypot recording-to-base from-base-by-mac update-to-base]]
            [my-sec-home.google-api :refer [upload-picture get-access-token refresh-token create-share-album erase-album]]
            [my-sec-home.confident :refer :all])
  (:gen-class))
 
#_(def beasts {"c44f333a6411" {:owner "svxWAYdQ9IqLzq1Sxde1Qw==" :pass (atom nil)}
             "3c71bfafc6ec" {:owner "svxWAYdQ9IqLzq1Sxde1Qw==" :pass (atom nil)}
             "cc50e394c7ec" {:owner "svxWAYdQ9IqLzq1Sxde1Qw==" :pass (atom nil)}
             "98f4ab00b34c" {:owner "svxWAYdQ9IqLzq1Sxde1Qw==" :pass (atom nil)}})
#_(def honeypot (atom {}))

#_(def save-pic (-> (fn [req] (pp/pprint (str (now) " >> " (:size ((:multipart-params req) "imageFile"))))
                    {:status 200
                     :headers {"Content-Type" "text/plain" "Content-Length" (str (count control-string))}
                     :body control-string})
                  wrap-params
                  wrap-multipart-params))

(defn request-example [req]
  {:status 200
   :headers {"Content-Type" "text/html"}
             ;"Content-Length" (str (count (str "Ready to " (:params req))))}
   :body (pp/pprint req)})
;
(defn send-command [pass] 
  (if-let [poket (@honeypot pass)]
    (let [order (:order poket)] 
      (reset! (:time-stamp poket) (t/now))
      {:status  200
       :headers {"Content-Type" "text/html"
                 "Content-Length" (str (count @order))}
       :body (let [answ @order] (reset! order "") #_(println answ) answ)})
    (do (println pass)
        {:status 200
         :headers {"Content-Type" "text/html"
                   "Content-Length" (str (count "to_runner: get_UUID"))}
         :body "to_runner: get_UUID"})))
;
(defn from-pet [pass params] (println params (:msg params))
  (if-let [msg (:msg params)] (do (if-let [gateway @(:gateway (@_owners (:owner (@honeypot pass))))] (deliver gateway params))
                                  ((fn [owner]
                                     (send-message
                                      owner
                                      (str (:alias (@(:zoo (@_owners owner)) (first (keep #(when (= @(:pass (val %)) pass) (key %)) @_beasts)))) ": " msg)))
                                   (:owner (@honeypot pass))))))
;
(defn entry-point [mac port remote-addr] (println remote-addr)
  (let [mac (Long/parseLong mac 16) dev (@_beasts mac false)]
    ((fn [%]
       {:status 200
        :headers {"Content-Type" "text/html" "Content-Length" (str (count %))}
        :body %})
     (if (false? dev) ""
         (let [new-pass (str (java.util.UUID/randomUUID)) bunch (@_owners (:owner dev)) acceptor (:acceptor (@(:zoo bunch) mac)) mode @(:mode (@(:zoo bunch) mac))]
           #_(reset! acceptor "") (reset! (:mode (@(:zoo bunch) mac)) :top-kb)
           (when (or (nil? @(:active bunch))
                     (and 
                      (not (= mac ( -> (:owner dev) ((deref _owners)) :active deref)))
                      (if-let [pass (-> (:owner dev) ((deref _owners)) :active deref ((deref _beasts)) :pass deref)]
                        (> (t/in-seconds (t/interval @(:time-stamp ((deref honeypot) pass)) (t/now))) 3)
                        true)))
             (reset! (:active bunch) mac))
           (when @(:pass dev)
             (when (= :stop-watchout-kb mode)
               (reset! (:mode (@(:zoo bunch) mac)) :stop-watchout-kb)
               (reset! acceptor "start_watchout")
               (if-let [give-token-at (:give-token-at (@honeypot @(:pass dev)))]
                 (when @give-token-at
                   (future (send-message
                            (:owner dev)
                            (str (:alias (@(:zoo (@_owners (:owner dev))) mac)) ": an error while sending a picture was caused to a restart. You can find missing photos on SD CARD")))))
               (future (send-message
                        (:owner dev)
                        (str (:alias (@(:zoo (@_owners (:owner dev))) mac)) " will go on watchout after restart"))))
             (swap! honeypot dissoc @(:pass dev)))
           (swap! honeypot conj {new-pass {:owner (:owner dev) :order acceptor :time-stamp (atom (t/now)) :give-token-at (atom nil) :port port}})
           (reset! (:pass dev) new-pass)
           (println (str (:alias (@(:zoo (@_owners (:owner dev))) mac)) " RESTARTS at: " (by-local-time (t/now))))
           (future (send-message (:owner dev) (str (:alias (@(:zoo (@_owners (:owner dev))) mac))
                                                   " RESTARTS at: " (by-local-time (t/now))
                                                   " Server status: " (:status (deref (http/get
                                                                                        (str "http://"
                                                                                             (if (= remote-addr "192.168.88.1") "patio.dp.ua" remote-addr)
                                                                                             ":"
                                                                                             port
                                                                                             "/just_respond"))))
                                                   ". Just type \"?!\" to choose the pet.")))
           new-pass)))))
;
(defn google-key [req]
  (handle-dump req)
  {:status 200
   :headers {"Content-Type" "text/html"}})
;
(def get-static (fn [req] 
                  {:status 200
                   :headers {"Content-Type" "text/plain"}
                   :body (io/input-stream (str "d:/clojure/my-sec-home/resources/public/patio.dp.ua/.well-known/acme-challenge/" (:file (:params req))))}))
;
(defn new-pet [mac owner] 
  (let [mac (Long/parseLong mac 16) pet-hangover (@_beasts mac)]
    (println pet-hangover)
    (if (and pet-hangover (not= (:owner pet-hangover) owner)) (send-message owner "This sensor is assigned to another owner. It's possible to connect no one's sensor only.")
        (let [new-alias (promise)]
          (send-message owner (if pet-hangover
                                (str "Type '##' to leave sensor his old name '" (:alias (@(:zoo (@_owners owner)) mac)) "' or type a new")
                                "Please givea name to sensor."))
          @(reset! (:gateway (@_owners owner)) new-alias)
          (when (not pet-hangover) 
            (let [pet {mac {:acceptor (atom "" #_(intern 'my-sec-home.core (gensym))) :alias @new-alias :status (atom :in-bed) :mode (atom :top-kb) :post-pic (atom nil)}}]
              (when (nil? (:zoo (@_owners owner))) (swap! _owners update-in [owner] conj {:zoo (atom {}) :active (atom nil)}))
              (println "WHERE!?" pet owner) 
              (swap! (:zoo (@_owners owner)) conj pet)
              (swap! _beasts conj {mac {:owner owner :pass (atom nil)}})
              (recording-to-base mac owner @new-alias)))
          (when (and pet-hangover (not= "##" @new-alias)) (println "TEST:" pet-hangover @new-alias)
            (update-to-base mac :alias @new-alias)
            (erase-album (:album_id (first (from-base-by-mac mac)))) ; It's a dummy at now
            #_(reset! (:alias (@(:zoo (@_owners owner)) mac)) @new-alias)
            (swap! (:zoo (@_owners owner)) update mac assoc :alias @new-alias) (println "TEST PASS"))
          (when (not= "##" @new-alias) 
            (let [album (create-share-album @new-alias)]
              (update-to-base mac :album_id (:id album))
              (send-message owner (str "Your link to photos: " (:shareableUrl (:shareInfo album))))
              (reset! (:acceptor (@(:zoo (@_owners owner)) mac)) (str "set_album: " (:id album)))))
          (send-message owner "Sensor is ready for using. Push SD card to the card-holder.")))))
;
(defn give-token [pass] #_(println 100) #_(pp/pprint pass)
  (let [token (get-access-token refresh-token)] 
    {:status 200
     :headers {"Content-Type" "text/plain"
               "Content-Length" (str (count token))}
     :body token}))
;
(def min-resp (constantly {:status 200}))
; :headers {"Content-Type" "text/html" "Content-Length" "4"} :body "WHY?"}))
;
(defroutes app-routes
  (GET "/my-sec-hm/watchout/give-token" [] give-token)
  (GET "/my-sec-hm/watchout/give-token/:pass" [pass] (do (reset! (:give-token-at (@honeypot pass)) (t/now)) (give-token pass)))
  (POST "/my-sec-hm/watchout/convey/:pass" [pass link comment queue] (let [owner (:owner (@honeypot pass)) 
                                                                           alias (:alias (@(:zoo (@_owners owner)) (first (keep #(when (= @(:pass (val %)) pass) (key %)) @_beasts))))]
                                                                       (println alias comment) #_(println link) #_(println (:owner (@honeypot pass)))
                                                                       (when (nil? queue) (reset! (:give-token-at (@honeypot pass)) nil))
                                                                       (future (send-picture owner alias link (str comment (when queue (str " +" queue)))))
                                                                       {:status 200}))
  (POST "/my-sec-hm/watchout/self-service/:pass" [pass p comment queue :as req] (do #_(pp/pprint req)
                                                                       (let [owner (:owner (@honeypot pass)) port (:port (@honeypot pass)) remote-addr(:remote-addr req)
                                                                             alias (:alias (@(:zoo (@_owners owner)) (first (keep #(when (= @(:pass (val %)) pass) (key %)) @_beasts))))]
                                                                         (println alias comment)
                                                                         (future
                                                                           (send-picture
                                                                            owner
                                                                            alias
                                                                            (str (if (= remote-addr "192.168.88.1") "http://patio.dp.ua:" (str "http://remote-addr" ":")) port "/:" p)
                                                                            #_(str "http://patio.dp.ua:"  port "/:" p)
                                                                            (str comment (when queue (str " +" queue))))))
                                                                       #_(Thread/sleep 3000)
                                                                     {:status 200 :headers {"Content-Type" "text/html" "Content-Length" "9"} :body "Hi, Jack!"}))
  (GET "/my-sec-hm/qr/:file-name" [file-name] {:status 200
                                               :headers {"Content-Type" "image/jpeg" "content-length" "67165" "accept-ranges" "bytes"}
                                               :body (io/input-stream (str "C:\\Users\\Intel\\AppData\\Local\\Temp\\" file-name))})
  (GET "/my-sec-hm/pics/:index" [index :as req] 
       {:status 200 :headers {"Content-Type" "image/jpeg"} :body (io/input-stream (str "C:\\Users\\Intel\\AppData\\Local\\Temp\\ring-multipart-" index ".tmp"))})

  (GET "/my-sec-hm/watchout/start/:mac" [mac p :as {remote-addr :remote-addr}] (entry-point mac p remote-addr))
  
  #_(POST "/my-sec-hm/watchout/runner-box/:pass" [pass :as {params :params}] (do #_(println pass) (future (from-pet params)) (send-command pass)))
  (POST "/my-sec-hm/watchout/runner-box/:pass" [pass & params :as {remote-addr :remote-addr}] (do #_(println pass) (when (seq params) (println params) (future (from-pet pass params))) (send-command pass)))

  (GET "/my-sec-hm/watchout/new-pet" [mac own] (do (future (new-pet mac own)) {:staus 200}))

  (POST "/my-sec-hm/watchout/upload/:pass"
        [pass :as #_req {{comment "comment" spots "spots" {file :tempfile} "file"} :multipart-params}]
        (do #_(pp/pprint comment)
            (send-message (:owner (@honeypot pass)) spots)
            (send-picture
             (:owner (@honeypot pass))
             (:alias (@(:zoo (@_owners (:owner (@honeypot pass)))) (first (first (filter #(= @(:pass (val %)) pass) @_beasts)))))
             (str "https://patio.dp.ua/my-sec-hm/pics/" (re-find #"\d+" (.getName (io/file file))))
             comment)
            )
        {:status 200})
  (GET "/my-sec-hm/watchout/test-pic/:count" [count] (do (println count) min-resp))

  (GET "/test/:file" [file :as req] (do #_(pp/pprint req) {:status 200 :headers {"Content-Type" "text/html; charset=utf-8"} :body (io/input-stream (str "C:/Users/Intel/esp/_esp-who/components/file_serving/" file))}))
  (GET "/sound" [] {:status 200 :headers {"Content-Type" "audio/wav"} :body (io/input-stream "D:\\Clojure\\my-sec-home\\resources\\2020-11-16 09-10-16.wav")})
;C:/Users/Intel/esp/_esp-who/components/file_serving/test.html
  (GET "/my-sec-hm/watchout/loader/:file" [file]
       {:status 200 :headers {"Content-Type" "application/octet-stream"} :body (io/input-stream (str "C:\\Users\\Intel\\esp\\simple_ota_example\\build\\" file ".bin"))})

  (POST "/my-sec-hm/confident/message-box" req (do #_(println "Got it!" req) (my-sec-home.confident/app-viber-hole req) #_{:status 200}))

  (ANY "/oauth2/google" []  my-sec-home.google-api/spec-handler)
  (ANY "/request" [] request-example) 
  (ANY "/.well-known/acme-challenge/:file" [file :as req] get-static)
  (ANY "/connect/google/success" [] google-key)
  (ANY "/oauth2callback" [] google-key)
  (ANY "/test" [] google-key)
  (ANY "/" []  {:status 200 :headers {"Content-Type" "text/html" "Content-Length" "9"} :body "Hi, Jack!"})
  (route/not-found "Error, page not found!!"))
;

(defonce server-tls 
  (jetty/run-jetty
   (wrap-defaults  #'app-routes (-> site-defaults
                                   (assoc-in [:security :anti-forgery] false)
                                   (assoc-in [:session :cookie-attrs :same-site] :lax)))
   {:join? false ;:async? true 
    :port 80
    :http? true
    :ssl? true
    :ssl-port 443
    :keystore "D://Clojure/patio/patio.pkcs12" :keystore-type "PKCS12" :key-password "*Bird*DDMM" }))

;

#_(defn send-photo [url]
  (http/post
   "https://chatapi.viber.com/pa/send_message"
   {:headers
    {"X-Viber-Auth-Token" "4aad877b3f67d16c-64346becb7847b01-2c9ba97ec8a68c31" "Content-Type" "application/json"}
    :body (json/write-str {:receiver "svxWAYdQ9IqLzq1Sxde1Qw=="
                           :min_api_version "1"
                           :sender {:name "ESP32-CAM HW"}
                           :type "picture"
                           :text "empty"
                           :media url}
                          :escape-slash false)}))

#_(defn sweet-pair []
  (let [rec {:start-point (atom 0) :interval (atom 0)}
        start-running (fn ([] {:start-point @(:start-point rec) :interval @(:interval rec)})
                        ([x] (reset! (:start-point rec) (t/now)) (reset! (:interval rec) x)))]
    start-running))
#_(defn check-expiration [] ((fn [{:keys [start-point interval]}] (neg? (- (t/in-seconds (t/interval start-point (t/now))) interval))) (start-running)))
;   (wrap-defaults handler (-> site-defaults (assoc-in [:session :cookie-attrs :same-site] :lax)))
#_(defn -main
  [& args]
  (let [port (Integer/parseInt (or (System/getenv "PORT") "443"))]
    ; Run the server with Ring.defaults middleware
    (defonce server
      (jetty/run-jetty
       (wrap-defaults #'app-routes (assoc-in site-defaults [:security :anti-forgery] false))
       {:join? false
        :ssl? false
        ;:ssl-port 443
        :keystore "D://Clojure/patio/patio.pkcs12" :keystore-type "PKCS12" :key-password "Vorona0905" }))
    (println (str "Running webserver at http:/127.0.0.1:" port "/"))))
;(defstate server :start (-main) :stop (.stop server))
#_(http/post
; "https://google.com"
 "https://chatapi.viber.com/pa/set_webhook" ;
; "http://127.0.0.1:8000/my-sec-hm/confident/message-box"
 {:headers {"X-Viber-Auth-Token" "4aad877b3f67d16c-64346becb7847b01-2c9ba97ec8a68c31" "Content-Type" "application/json"}
  :body (json/write-str {:url "https://patio.dp.ua/my-sec-hm/confident/message-box"
                         :send-name true :send-photo true} :escape-slash false)}
 (fn [{:keys [opts status headers body error]}]
   (println "Opts --" opts)
   (println "Status --" status)
   (println "Headers --" headers)
   (println "Body --" body)))

#_(clojure.java.io/copy
 (:body (http/get "http://192.168.88.180/capture?_cb123" {:as :stream}))
 (java.io.File. "testtest.jpg"))

#_(dotimes [i 10]
  (println "Request #" (inc i))
  (let [{:keys [status headers body error] :as rep} 
        (http/get "http://192.168.88.180/capture?_cb123" {:as :stream})]
        (println error)
        (play (read-sound "src/my_sec_home/362650__ethraiel__soft-alert.wav"))))

#_(defn series [len]
  (println "Request #" len)
  (if (zero? len) (play (read-sound "end-of-series.wav"))
      (http/get "http://192.168.88.180/capture?_cb123" {:as :stream}
                (fn [{:keys [status headers body error]}]
                  (println "status:" status)
                  (println "header:" headers)
                  (println "body:" body)
                  (if (nil body) (play (read-sound "fail-down.wav"))
                      (clojure.java.io/copy
                       body
                       (java.io.File.
                        (clojure.string/join
                         (list
                          "resources/"
                          (clojure.string/replace (str (now)) #":" "-")
                          ".jpg"))))
                      (println "error:" error)
                      (play (read-sound "src/my_sec_home/362650__ethraiel__soft-alert.wav")))
                  (series (dec len))))))
    


  #_                      (java.io.File.
                                (clojure.string/join
                                 (list
                                  "resources/"
                                  (clojure.string/replace (str (now)) #":" "-")
                                  ".jpg")))
    #_(play (read-sound "src/my_sec_home/362650__ethraiel__soft-alert.wav"))
           


;
(defn my-wrap [fn-name & args]
  (println "here" args)
  (println ((first args) :body))
  (apply fn-name ((first args) :body)))

#_(clojure.java.io/copy
                     (body-string p)
                     (java.io.File.
                      (clojure.string/join
                       (list
                        "resources/"
                        (clojure.string/replace (str (now)) #":" "-")
                          ".jpg"))))

                                        ;(pp/pprint (:params req))
#_(let [{:keys [body error]} req]
    (if (nil? body) (play (read-sound "fail-down.wav"))
        (println (body-string body)))
        ;  (println "error:" error)
          ;(play (read-sound "src/my_sec_home/362650__ethraiel__soft-alert.wav"))))
    {:status  200
     :headers {"Content-Type" "text/html"}
     :body    (do
               (pp/pprint req)
               (str "Request Object: " req))})
                    #_(shell/sh "C:\\WINDOWS\\system32\\WindowsPowerShell\\v1.0\\powershell.exe"
                              (str "mv "
                                    (io/.getPath (:tempfile (:imageFile (:params req))))
                                    " "
                                    ".\\resources\\("
                                    (clojure.string/replace 
                                     (clojure.string/replace (str (now)) #":" "-")
                                     " " "~")
                                    ".jpg"))
;status_message "ok"
;   :headers {"Content-Type" "text/html"}})
;   :body    (->>
;                (pp/pprint req)
;                (str "Request Object: " req))})
;   :event-types ["delivered" "seen" "failed" "subscribed" "unsubscribed" "conversation_started"]
;   :headers {"Content-Type" "text/html"}
;   :body    (->>
;             (pp/pprint (:params req))
;             (str "Request Object: " req))})
#_(GET "/my-sec-hm/watchout/give-token" [] give-token)
#_(POST "/my-sec-hm/watchout/convey/:pass" [pass link comment] (let [owner  (:owner (@honeypot pass))]
                                                                 (println pass "@@@@@@@@@@@@@@@@@!!!!!!!!!!!@@###########")
                                                                 #_(my-sec-home.confident/send-picture 1 2 3 4)
                                                                 owner "123" 
                                                                 (:alias ((:zoo (owners owner)) (first (first (take-while #(= @(:pass (val %)) pass) beasts)))))
                                                                 link
                                                                 comment
                                                                 "123"
                                                                 min-resp))
  #_(GET "/my-sec-hm/watchout/loader/:file" [file]
       {:status 200 :headers {"Content-Type" "application/octet-stream"} :body (io/input-stream (str "C:\\Users\\Intel\\esp\\simple_ota_example\\build\\" file ".bin"))})

