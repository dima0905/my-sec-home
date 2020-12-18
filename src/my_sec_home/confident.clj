(ns my-sec-home.confident
  (:require [clojure.data.json :as json]
            [org.httpkit.client :as http]
            [ring.middleware.json :refer [wrap-json-body]]
            [ring.middleware.keyword-params :refer [wrap-keyword-params]]
            [clj-time.core :as t]
            [clj-time.format :as f]
            [clj.qrgen :refer :all]
            [clojure.pprint :as pp]
            [my-sec-home.database :refer [_owners _beasts honeypot deleting-from-base from-base-by-mac]]
            [my-sec-home.google-api :refer [upload-picture my-album-id QR-codes-album albums-link]]
            [my-sec-home.kb :refer :all]))
;
(def custom-time-formatter (f/with-zone (f/formatter "yyyy-MM-dd hh:mm:ss") (t/default-time-zone)))
(defn now [] (f/unparse custom-time-formatter (t/now)))
(defn by-local-time [time] (f/unparse custom-time-formatter (t/now)))
;
(def orders-to-pet ["to_runner:" ["get_UUID" "restart all" "get state" "invert mode" "get access" "block access" "OTA updates"]
                    "--help"
                    "launch"
                    "take_photo"
		    "start_watchout"
		    "stop_watchout"
		    "set_sensivity" [0 255]
		    "get_sensivity"
		    "set_max_area" [0 307200]
		    "get_max_area"
                    "set_led" [0 4096]
                    "set_color_bar" [0 1]
                    "set_vflip" [0 1]
                    "get_reg" ['bank 'reg]
                    "set_reg" ['bank 'reg 'val]
		    "set_album:"])
;
#_(def owners {"svxWAYdQ9IqLzq1Sxde1Qw=="
             {:zoo
              {"c44f333a6411" {:acceptor (atom (intern 'my-sec-home.core (gensym))) :alias "belka" :status (atom :in-bed) :mode (atom :forgotten) :post-pic (atom nil)}
               "98f4ab00b34c" {:acceptor (atom (intern 'my-sec-home.core (gensym))) :alias "armstrong" :status (atom :in-bed) :mode (atom :forgotten) :post-pic (atom nil)}
               "3c71bfafc6ec" {:acceptor (atom (intern 'my-sec-home.core (gensym))) :alias "gagarin" :status (atom :in-bed) :mode (atom :forgotten) :post-pic (atom nil)}
               "cc50e394c7ec" {:acceptor (atom (intern 'my-sec-home.core (gensym))) :alias "titov" :status (atom :in-bed) :mode (atom :forgotten) :post-pic (atom nil)}}
              :active (atom "cc50e394c7ec")}})
;
;
(def VIBER-API-ENDPOINTS {:send-message "https://chatapi.viber.com/pa/send_message" :get-online "https://chatapi.viber.com/pa/get_online"})
;
(def MY-VIBER-ID "svxWAYdQ9IqLzq1Sxde1Qw==")
;
(defn send-message [owner message]
  (http/post (:send-message VIBER-API-ENDPOINTS)
             {:headers {"X-Viber-Auth-Token" "4aad877b3f67d16c-64346becb7847b01-2c9ba97ec8a68c31" "Content-Type" "application/json"}
              :body (json/write-str {:receiver owner :min_api_version 3
                                     :type "text" :text message}
                                    :escape-slash false)}))
;
(defn send-kb [owner kb] 
  (http/post (:send-message VIBER-API-ENDPOINTS)
             {:headers {"X-Viber-Auth-Token" "4aad877b3f67d16c-64346becb7847b01-2c9ba97ec8a68c31" "Content-Type" "application/json"}
              :body (json/write-str {:receiver owner :min_api_version 3
                                     :keyboard
                                     (update-in 
                                      (if (= 1 (count  @(:zoo (@_owners owner)))) #_(empty?
                                           (drop-while
                                            #(or (false? %) (nil? %))
                                            (rest
                                             (drop-while
                                              #(or (false? %) (nil? %))
                                              (map (fn [mac] ((fn [state]
                                                                (when state (< (t/in-seconds (t/interval @state (t/now))) 3)))
                                                              (:time-stamp (@honeypot @(:pass (@_beasts mac))))))
                                                   (keys @(:zoo (@_owners owner))))))))
                                        (my-sec-home.kb/kb kb)
                                        (update-in (my-sec-home.kb/kb kb) [:Buttons] conj my-sec-home.kb/change-button))
                                      [:Buttons] 
                                      #(vec (map (fn [btn] (update-in btn [:Text] clojure.string/replace #"@@@@@" ((fn [%1] (:alias (@(:zoo %1) @(:active %1)))) (@_owners owner)))) %)))}
                                         ;(update-in my-sec-home.confident/change-button [:Text] clojure.string/replace #"@@@@@" (#(:alias ((:zoo %) @(:active %))) (owners owner)))))}
                                    :escape-slash false)}
             (fn [{:keys [body]}] #_(pp/pprint (json/read-str body))))
  (reset! ((fn [%] (:mode (@(:zoo %) @(:active %)))) (@_owners owner)) kb))
;
(defn send-picture [owner alias link comment] #_(println owner alias link comment)
  (let [pet ((fn [%] (@(:zoo %) @(:active %))) (@_owners owner))]
    (http/post (:send-message VIBER-API-ENDPOINTS)
               {:headers {"X-Viber-Auth-Token" "4aad877b3f67d16c-64346becb7847b01-2c9ba97ec8a68c31" "Content-Type" "application/json"}
                :body (json/write-str {:receiver owner :type "picture" :text (str alias " at: " comment) :media link})}
               (fn [_]
                 (reset! (:post-pic pet) (list #'send-kb (list owner @(:mode pet))))))))
;
(defn get-qr [owner [pat arg]] 
  (http/post (:send-message VIBER-API-ENDPOINTS)
             {:headers {"X-Viber-Auth-Token" "4aad877b3f67d16c-64346becb7847b01-2c9ba97ec8a68c31" "Content-Type" "application/json"}
              :body (json/write-str {:receiver owner
                                     :type "picture"
                                     :text "Show it to sensor"
                                     :media (upload-picture
                                             (as-input-stream (from (clojure.string/replace arg (re-pattern pat) "\n") :size [400 400] :image-type JPG))
                                             QR-codes-album)}
                                     #_(str "https://patio.dp.ua/my-sec-hm/qr/"
                                                 (.getName (as-file (from (clojure.string/replace arg (re-pattern pat) "\n") :size [400 400] :image-type JPG))))
                                    :escape-slash false)}))
;                                        
(defn chosen [owner mac]  
  (let [bunch (@_owners owner) mac (Long/parseLong mac)] 
    (reset! (:active bunch) mac)
    (send-kb owner @(:mode (@(:zoo bunch) mac))))) 
;                                        
(defn choice-of-active [owner set] #_(println "CHOICE-OF-ACtIVE:" owner (keyword filter))
  (if (or (empty? (@_owners owner)) (nil? (:zoo (@_owners owner))) (empty? @(:zoo (@_owners owner))))
    (send-message owner "You don't have any sensors. Type 'launch' if you wanna connect the sensor.")
    (if (= 1 (count  @(:zoo (@_owners owner))))
      (chosen owner (str (first (keys  @(:zoo (@_owners owner))))))
      (http/post (:send-message VIBER-API-ENDPOINTS)
                 {:headers {"X-Viber-Auth-Token" "4aad877b3f67d16c-64346becb7847b01-2c9ba97ec8a68c31" "Content-Type" "application/json"}
                  :body (json/write-str
                         {:receiver owner :min_api_version 3
                          :keyboard
                          {:Type "keyboard"
                           :Buttons
                           (let [buttons 
                                 (vec (map (fn [[mac {alias :alias}] bg-color]
                                             {:Colums 3
                                              :Rows 1
                                              :Text (str "<font color=\"#494E67\">Choose </font><b>" alias "</b>")
                                              :TextSize "large"
                                              :TextHAlign "center"
                                              :TextVAlign "middle"
                                              :ActionType "reply"
                                              :ActionBody (str "chosen " mac)
                                              :BgColor bg-color})
                                           (filter (if (= set "active")
                                                     (fn [[mac _]] ((fn [state]
                                                                      (when state (< (t/in-seconds (t/interval @state (t/now))) 3)))
                                                                    (:time-stamp (@honeypot @(:pass (@_beasts mac))))))
                                                     identity)
                                                   @(:zoo (@_owners owner)))
                                           '("#f6fafa" "#e6f0f0" "#d6e6e6" "#c6dddd" "#b6d3d3" "#f6fafa" "#e6f0f0" "#d6e6e6" "#c6dddd" "#b6d3d3")))]
                             #_(println buttons) buttons)}}
                         :escape-slash false)}))))
                                        ;
(defn escape [owner] (send-kb owner :top-kb))
;
(defn refuse [owner]
  (let [bunch (@_owners owner) mac @(:active bunch) pass @(:pass (@_beasts mac))]
    (reset! (:active bunch) nil)
    (swap! (:zoo bunch) dissoc mac)
    (swap! _beasts dissoc mac)
    (deleting-from-base mac)
    (when pass 
      (reset! (:order (@honeypot pass)) "to_runner: restart all")
      (future (Thread/sleep 3000) (swap! honeypot dissoc pass)))
    (choice-of-active owner "all")))
;
(defn threshold [owner param] #_(println param)
  (let [val (promise) gateway (:gateway (@_owners owner)) sens (= param "sensivity")]
    (reset! (:acceptor (#(@(:zoo %) @(:active %)) (@_owners owner))) (if sens "get_sensivity" "get_max_area"))
    @(reset! gateway val)
    (let [val (:msg @val)] #_(println val)
         (send-message owner (str
                              "Input a new "
                              (if sens "SENSIVITY " "MAX AREA ")
                              "threshold value. From "
                              (if sens "6 " "1 ")
                              "to "
                              (if sens "255 " "307200 ")
                              "Current value is: " (re-find #"\d+" val)))
          (let [val (promise)]
            @(reset! gateway val)
            (if (re-find #"\D+" @val)
              (send-message owner "The value contains a non-digital character")
              (let [ival (Long/parseLong @val 10)]
                    (if (if sens (or (< ival 6) (> ival 255)) (or (< ival 1) (> ival 307200)))
                      (send-message owner "The inputted value is out of range")
                      (reset! (:acceptor (#(@(:zoo %) @(:active %)) (@_owners owner))) (if sens (str "set_sensivity " @val) (str "set_max_area " @val))))))))))
;
(defn get-link [owner] (send-message owner (albums-link (:album_id (first (from-base-by-mac @(:active (@_owners owner))))))))
(defn wrap-erase-album [owner] (my-sec-home.google-api/erase-album (:album_id (first (from-base-by-mac @(:active (@_owners owner)))))))
;
(defn test-message [req] (pp/pprint req))
;
(defn settings [owner] (send-kb owner :settings-kb))
;
(defn ?! [owner] #_(println owner) (choice-of-active owner "all"))
;
(defn start-watchout [owner]
  (let [bunch (#(@(:zoo %) @(:active %)) (@_owners owner))]
    (reset! (:acceptor bunch) "start_watchout")
    (send-kb owner :stop-watchout-kb)
    (reset! (:mode bunch) :stop-watchout-kb)
    (future (let [time-stamp (:time-stamp (@honeypot (-> owner ((deref _owners)) :active deref ((deref _beasts)) :pass deref)))
                  active (#(:alias (@(:zoo %) @(:active %))) (@_owners owner))]
              (while (< (t/in-seconds (t/interval @time-stamp (t/now))) 10) (Thread/sleep 3000))
              (send-message owner (str active " has gone off the radar."))))
    #_{:status 200})) 
;
(defn stop-watchout [owner]
  (let [pet (#(@(:zoo %) @(:active %)) (@_owners owner))]
    (reset! (:acceptor pet) "stop_watchout")
    (send-kb owner :top-kb)
    (reset! (:mode pet) :top-kb)
    #_{:status 200}))
;
(defn take-photo [owner] 
  (let [pet (#(@(:zoo %) @(:active %)) (@_owners owner))]
    (reset! (:acceptor pet) "take_photo")
    (reset! (:mode pet) :top-kb)
    #_{:status 200}))
;
(defn message [{{owner :id} :sender {text :text media :media} :message}] #_(println media) 
  (let [[order & params] (clojure.string/split text #" ")] 
    (if-let [proc (ns-resolve 'my-sec-home.confident (symbol order))]  (do #_(println (cons owner params))
      (apply proc (#(if (empty? %) (list owner) (cons owner %)) params)))
      (if (not (neg? (.indexOf orders-to-pet order)))
        (reset! (:acceptor (#(@(:zoo %) @(:active %)) (@_owners owner))) text)
        (deliver @(:gateway (@_owners owner)) text)))))
;
(defn delivered [req] #_(println "My message was delivered: " req) #_{:status 200})
;
(defn failed [req] (println "My message was failed: " req) #_{:status 200})
;
(defn seen [req] #_(println "User have seen my message" req)
  (when (@_owners (:user_id req))
    (let [post-pic (:post-pic (#(@(:zoo %) @(:active %)) (@_owners (:user_id req))))]
      (when (not (empty? @post-pic))
        (apply (first @post-pic) (second @post-pic))
        (reset! post-pic nil)
        {:status 200}))))
;
(defn launch [owner]
  (when (not (contains? @_owners owner)) (swap! _owners conj {owner {:gateway (atom nil)}}))
  (let [ssid (promise) gateway (:gateway (@_owners owner))]
    (send-message owner "SSID?")
    @(reset! gateway ssid)
    (let [password (promise)]
      (send-message owner "password?")
      @(reset! gateway password)
      (send-message owner "Remove the card from the card-holder and restart the sensor.")
      (get-qr owner [">" (str @ssid ">" @password ">" owner)]))))
;
(defn --help [owner] (send-message owner (prn-str orders-to-pet)))
;
(defn conversation_started [{{user :id name :name} :user :as req}] (println req)
  (http/post (:send-message VIBER-API-ENDPOINTS)
             {:headers {"X-Viber-Auth-Token" "4aad877b3f67d16c-64346becb7847b01-2c9ba97ec8a68c31" "Content-Type" "application/json"}
              :body (json/write-str {:receiver user :min_api_version 3 :keyboard greeting-kb} :escape-slash false)})
  {:status 200})
;
(defn subscribed [params] (println "subscribed: " params) {:status 200})
;
(defn unsubscribed [params] (println "unsubscribed: " params) {:status 200})
;
(defn wrap-body [handler] (fn [req] (-> req (get :body) handler)))
;
(def app-viber-hole (-> (fn [m] #_(pp/pprint m) (future (apply (ns-resolve 'my-sec-home.confident (symbol (:event m))) (list m))) {:status 200})
                        wrap-body
                        (wrap-json-body {:keywords? true})))
;
;
;
;                                        
(def breaking-news (atom nil))
(defn pass-on [_ _ _ message] (println message) (send-picture MY-VIBER-ID (:link message) (:comment message)))
(add-watch breaking-news :got-picture pass-on)
#_(fn [{:keys [opts status headers body error]}]
  (println "Opts --" opts)
  (println "Status --" status)
  (println "Headers --" headers)
  (println "Body --" body))

