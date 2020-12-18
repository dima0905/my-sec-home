;;
(ns my-sec-home.google-api
  (:require [org.httpkit.client :as http]
            [clojure.pprint :as pp]
            [ring.util.codec :refer :all]
            [ring.middleware.oauth2 :refer :all]
            [clojure.data.json :as json]
            [clj-time.core :as t]
            [clj-time.format :as f]
            [clojure.java.io :as io]
            [my-sec-home.database :refer [_owners _beasts honeypot deleting-from-base from-base-by-mac]]))
;
(def last-google-creds {"access_token" "ya29.a0AfH6SMADVoG4LeaU5Juz9Qvua2QDh6FrhP3BBw2Im5awpBC2D4ycGirGadUydvF8mft98RoJ9_25MFc7x6yk5IrfDVpgLRiGSbSN8qgaUz2OtYrTf-Bv4XO_haQQ_TYscy4t9UEzpy1xk7volPglAvwF77MlYm0YvvQ", "expires_in" 3599, :refresh_token "1//0cTczWVxJcqo6CgYIARAAGAwSNwF-L9Ir83U3cHEeuQ-5BcZX1w1ngRz02-VrfcsJYULESef9I7ABYaOxxY_n6SS7HvbSxrpd9uo", "scope" "https://www.googleapis.com/auth/photoslibrary https://www.googleapis.com/auth/photoslibrary.sharing", "token_type" "Bearer"})
;
(def API-KEY "AIzaSyBNRgRe4AR5aCCAy8AURDyUoHQCHL5nDMs") ;"AIzaSyAtVuCDgvx_GXUDHCajgnFa91kjB7pQfBU")  ;AIzaSyAUi4GgiGTMrnIlR2tpS8mV6i-kPudi7kQ
;
(def PHOTOS-API-ENDPOINTS
  {:refresh-token "https://oauth2.googleapis.com/token"
   :create-album "https://photoslibrary.googleapis.com/v1/albums"
   :upload-binary "https://photoslibrary.googleapis.com/v1/uploads"
   :creating-media-item "https://photoslibrary.googleapis.com/v1/mediaItems:batchCreate"
   :albums-list "https://photoslibrary.googleapis.com/v1/albums"
   :get-item "https://photoslibrary.googleapis.com/v1/mediaItems"
   :get-item-by-album "https://photoslibrary.googleapis.com/v1/mediaItems:search"
   :my-endpoint "https://localhost/request"})
;
(def my-album-id "AGD8o9Tu7Yovhd4NnOcKuQUQsI4hmn5Cj-Z57qEnrhoJUMf1I9xYrZ6p9ezEINcUiW2_NkofJA5c") ; "qwe"
;
(def QR-codes-album "APtE3BDAt8m2T8pNRift5VttDgm-mHyOwCPOBoZ1XJGE0ypWrc1hPCrcSsq01--9Xo1ViU3y5BHW")
(def oauth2-params
  {:client-id "843507003240-kqsf1g4unv9u56aas5va5difoot15sbe.apps.googleusercontent.com" ;"835866718456-7f51dkg9avtn3jghjefioo45vmibrqmv.apps.googleusercontent.com"
   :client-secret "lUMcESIfiXc2J_0NdoYuxBQp" ;"nWRN4-W_KjQ8dDFCCEFCtWmn"
   :authorize-uri  "https://accounts.google.com/o/oauth2/v2/auth"
;   :redirect-uri "https://patio.dp.ua/connect/google/success"
   :redirect-uri "http://localhost"
   :access-token-uri "https://oauth2.googleapis.com/token"
   :scope "https://www.googleapis.com/auth/photoslibrary"})
;
(defn refresh-token-url [refresh-token]
  (str
   "?client_id=" (url-encode (:client-id oauth2-params))
   "&client_secret=" (url-encode (:client-secret oauth2-params))
   "&refresh_token=" (url-encode refresh-token)
   "&grant_type=refresh_token"))
;
(def refresh-token #_"ya29.a0AfH6SMBuv7ObLVm8q-pz4bvE-GnWjfwGT4s2yuMfH5xjgUOFTVDx17ZZHxC3sbQA8zt_Wkzh_FTJKKPl4PI0vSX9wEHugtTBsyDjPplA5oTvrTP1N8k4pFx8CEV9XQ4HObddLlNotHpqiVp7Wa0Q4FOT7yLvH1BK5WC_" "1//0cTczWVxJcqo6CgYIARAAGAwSNwF-L9Ir83U3cHEeuQ-5BcZX1w1ngRz02-VrfcsJYULESef9I7ABYaOxxY_n6SS7HvbSxrpd9uo"
  #_"ya29.a0AfH6SMADVoG4LeaU5Juz9Qvua2QDh6FrhP3BBw2Im5awpBC2D4ycGirGadUydvF8mft98RoJ9_25MFc7x6yk5IrfDVpgLRiGSbSN8qgaUz2OtYrTf-Bv4XO_haQQ_TYscy4t9UEzpy1xk7volPglAvwF77MlYm0YvvQ")
;
(defn _get-access-token []
  (let [token (atom nil) start-point (atom nil) interval (atom nil)
        get-access-token
        (fn [refresh-token]
          (when (or (nil? @token) (nil? @start-point) (> (t/in-seconds (t/interval @start-point (t/now))) @interval)) #_(println "start cond complete")
                (let [{:keys [access_token expires_in]}
                      (json/read-str (:body @(http/post (str (:refresh-token PHOTOS-API-ENDPOINTS) (refresh-token-url refresh-token)))) :key-fn keyword)]
              (reset! token access_token) (reset! start-point (t/now)) (reset! interval (- expires_in 33))))
            @token)]
    get-access-token))

(def get-access-token (_get-access-token)) 
;
(defn erase-album [album-id] #_(println album-id);!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
  (loop [request {:pageSize "49" :albumId album-id}] 
    (let [{body :body} @(http/post (:get-item-by-album my-sec-home.google-api/PHOTOS-API-ENDPOINTS)
                                   {:headers
                                    {"Authorization" (str "Bearer " (get-access-token refresh-token))
                                     "Content-type" "application/json"}
                                    :body (json/write-str request)})
          {:keys [mediaItems nextPageToken]} (json/read-str body :key-fn keyword)]
;;      (doseq [{id :id} mediaItems]
      (http/post (str "https://photoslibrary.googleapis.com/v1/albums/" album-id ":batchRemoveMediaItems")
                 {:headers
                  {"Authorization" (str "Bearer " (get-access-token refresh-token))
                   "Content-type" "application/json"}
                  :body (json/write-str {:mediaItemIds (mapv :id mediaItems)})} pp/pprint)
      (if (nil? nextPageToken) "The end" (recur (conj request {:pageToken nextPageToken}))))))
;
(defn upload-data [data]
  {:headers
   {"Authorization" (str "Bearer " (get-access-token refresh-token))
    "Content-Type" "application/octet-stream"
    "X-Goog-Upload-File-Name" "image/jpeg"
    "X-Goog-Upload-Protocol" "raw"}
   :body data})
;
(defn creating-media-item [token album-id file-name]
  {:headers
   {"Authorization" (str "Bearer " (get-access-token refresh-token))
    "Content-type" "application/json"}
   :body (json/write-str
          {:albumId album-id
           :newMediaItems [{:description "The best of the best cat" :simpleMediaItem {:fileName (str file-name ".jpg") :uploadToken token}}]})})
;
(defn share-album []
  {:headers
   {"Authorization" (str "Bearer " (get-access-token refresh-token))
    "Content-type" "application/json"}
   :body (json/write-str {:sharedAlbumOptions {:isCollaborative true :isCommentable true}})})
  
(def get-item
  (fn []
    {:headers
     {"Authorization" (str "Bearer " (get-access-token refresh-token))
      "Accept" "application/json"}}))
;
(defn upload-picture [data album]
  (let [{token :body} @(http/post (:upload-binary PHOTOS-API-ENDPOINTS) (upload-data data))]
    (let [{body :body} @(http/post (:creating-media-item PHOTOS-API-ENDPOINTS) (creating-media-item token album "tmp.jpg"))]
      #_(println (:id (:mediaItem ((:newMediaItemResults (json/read-str body :key-fn keyword)) 0))))
      (let [{body :body} @(http/get (str (:get-item PHOTOS-API-ENDPOINTS) "/" (:id (:mediaItem ((:newMediaItemResults (json/read-str body :key-fn keyword)) 0)))
                                         "?key=" API-KEY) (get-item))]
        #_(-> body println wrap-json-body)
        (:baseUrl (json/read-str body :key-fn keyword))))))
                                        ;
(defn albums-list [refresh-token]
  {:headers
   {"Authorization" (str "Bearer " (get-access-token refresh-token))
    "Accept" "application/json"}})

(defn albums-link [id]
  (let [{body :body} @(http/get (str (:albums-list my-sec-home.google-api/PHOTOS-API-ENDPOINTS) "/" id) (albums-list refresh-token))]
    (:shareableUrl (:shareInfo (json/read-str body :key-fn keyword)))))

(defn create-share-album [title]
  (let [{body1 :body} @(http/post (:create-album PHOTOS-API-ENDPOINTS)
                                     {:headers
                                      {"Authorization" (str "Bearer " (get-access-token refresh-token))
                                       "Accept" "application/json"
                                       "Content-Type" "application/json"}
                                      :body (json/write-str {:album {:title title}})})]
    (let [{:keys [body]} @(http/post (str (:create-album PHOTOS-API-ENDPOINTS) "/" (:id (json/read-str body1 :key-fn keyword)) ":share")
                                     {:headers
                                      {"Authorization" (str "Bearer " (get-access-token refresh-token))
                                       "Content-type" "application/json"}
                                      :body (json/write-str {:sharedAlbumOptions {:isCollaborative false :isCommentable false}})})]
      (conj (json/read-str body1 :key-fn keyword) (json/read-str body :key-fn keyword)))))
;
(defn upload-binary [name-in]
  {:headers
   {"Authorization" (str "Bearer " (get-access-token refresh-token))
    "Content-Type" "application/octet-stream"
    "X-Goog-Upload-File-Name" "image/jpeg"
    "X-Goog-Upload-Protocol" "raw"}
   :body (io/input-stream name-in)})
;
#_(defn upload-array [array]
  {:headers
   {"Authorization" (str "Bearer " (get-access-token refresh-token))
    "Content-Type" "application/octet-stream"
    "X-Goog-Upload-File-Name" "image/jpeg"
    "X-Goog-Upload-Protocol" "raw"}
   :body (as-input-stream array)})
;
(def spec-handler
  (wrap-oauth2
   pp/pprint
   {:google
    {:authorize-uri "https://accounts.google.com/o/oauth2/v2/auth"
     :access-token-uri "https://oauth2.googleapis.com/token"
     :client-id "835866718456-7f51dkg9avtn3jghjefioo45vmibrqmv.apps.googleusercontent.com"
     :client-secret "nWRN4-W_KjQ8dDFCCEFCtWmn"
     :scopes ["https://www.googleapis.com/auth/photoslibrary" "https://www.googleapis.com/auth/photoslibrary.sharing"]
     :launch-uri "/oauth2/google"
     :redirect-uri "http://localhost"
     :landing-uri "/test"
     :basic-auth? true}}))
;---------------------   

;(def refresh-token "1//0cgbprimZM3pjCgYIARAAGAwSNwF-L9IrBdQZ6L39NQN9zc6wlxqrJLSYy4FKJhuWtGi7rMlE0SEsugD_duU_lKoamFurcqAzMdw")
;(def refresh-token "1//0cL7NEo0Nd-4GCgYIARAAGAwSNwF-L9IrkXxkLWdr-GQaNESOrB15HK2Ra5morM16TipkvafO4_coNM8_uxj1ImmaULVpGKYXGeU")
(defn authorize-uri [client-params csrf-token]
  (str
   (:authorize-uri client-params)
   "?response_type=code"
   "&client_id="
   (url-encode (:client-id client-params))
   "&redirect_uri="
   (url-encode (:redirect-uri client-params))
   "&scope="
   (url-encode (:scope client-params))
   "&state="
   (url-encode csrf-token)
   "&access_type=offline"))

(defn get-token [client-params]
  (str
   (:access-token-uri client-params)
   "?code="
   (url-encode "4/0QGgkWqpwgspMeF_5_KB-klALJpRhg-zRSesSC7ugZqc-A4PuqOHzg4-Cq65g495y3lZ_ukiWnl0pK-dtoS51yg")
   "&client_id="
   (url-encode (:client-id client-params))
   "&client_secret="
   (url-encode (:client-secret client-params))
   "&redirect_uri="
   (url-encode (:redirect-uri client-params))
   "&grant_type=authorization_code"))

(defn get-authentication-response [csrf-token response-params]
  (if (= csrf-token (:state response-params))
    (try
      (-> (http/post (:access-token-uri oauth2-params)
                     {:form-params {:code         (:code response-params)
                                    :grant_type   "authorization_code"
                                    :client_secret (:client-secret oauth2-params)
                                    :client_id    (:client-id oauth2-params)
                                    :redirect_uri (:redirect-uri oauth2-params)}
                      ;:basic-auth [(:client-id oauth2-params) (:client-secret oauth2-params)]
                      :as          :json
                      })
          :body)
      (catch Exception _ nil))
    nil))
;
#_(def oauth2-params
  {:client-id "835866718456-7f51dkg9avtn3jghjefioo45vmibrqmv.apps.googleusercontent.com"
   :client-secret "nWRN4-W_KjQ8dDFCCEFCtWmn"
   :authorize-uri  "https://accounts.google.com/o/oauth2/v2/auth"
;   :redirect-uri "https://patio.dp.ua/connect/google/success"
   :redirect-uri "http://localhost"
   :access-token-uri "https://oauth2.googleapis.com/token"
   :scope "https://www.googleapis.com/auth/photoslibrary"})
;
#_(def oauth2-params
  {:client-id "835866718456-7f51dkg9avtn3jghjefioo45vmibrqmv.apps.googleusercontent.com"
   :client-secret "nWRN4-W_KjQ8dDFCCEFCtWmn"
   :authorize-uri  "https://accounts.google.com/o/oauth2/v2/auth"
;   :redirect-uri "https://patio.dp.ua/connect/google/success"
   :redirect-uri "http://localhost"
   :access-token-uri "https://oauth2.googleapis.com/token"
   :scope "https://www.googleapis.com/auth/photoslibrary"})
