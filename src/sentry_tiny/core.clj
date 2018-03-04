(ns sentry-tiny.core
  (:require
    [clojure.string :as str]
    [clojure.java.io :as io]
    [org.httpkit.client :as http]
    [cheshire.core :as json]
    [clj-uuid :as uuid]
    [clj-time
     [core :as t]
     [format :as ft]])
  (:import
    (java.net InetAddress)))

(defonce ^:private fallback (atom {:enabled? false}))

(def ^:private hostname
  (delay
    (try
      (.getHostName (InetAddress/getLocalHost))
      (catch Exception _ "unknown"))))

(defonce ^{:private true
           :const   true
           :static  true}
         lib-version
         (or (some-> "VERSION" io/resource slurp str/trim)
             "x.y.z-devel"))

(defonce ^{:private true
           :const   true
           :static  true}
         client-name
         (str "sentry-tiny/" lib-version))

(defn- make-frame [^StackTraceElement element app-namespaces]
  {:filename (.getFileName element)
   :lineno   (.getLineNumber element)
   :function (str (.getClassName element) "." (.getMethodName element))
   :in_app   (boolean (some #(.startsWith (.getClassName element) %) app-namespaces))})

(defn- make-stacktrace-info [elements app-namespaces]
  {:frames (reverse (map #(make-frame % app-namespaces) elements))})

(defn- add-stacktrace [event-map ^Exception e & [app-namespaces]]
  (assoc event-map
    :exception
    [{:stacktrace (make-stacktrace-info (.getStackTrace e) app-namespaces)
      :type       (str (class e))
      :value      (.getMessage e)}]))

(defn- generate-uuid []
  (str/replace (uuid/v4) #"-" ""))

(defn- make-sentry-url [uri project-id]
  (format "%s/api/%s/store/" uri project-id))

(defn- make-sentry-header [ts key secret]
  (str "Sentry sentry_version=2.0, "
       "sentry_client=" client-name ", "
       "sentry_timestamp=" ts ", "
       "sentry_key=" key ", "
       "sentry_secret=" secret))

(defn- send-event [{:keys [ts uri project-id key secret]} event-info]
  (let [url (make-sentry-url uri project-id)
        header (make-sentry-header ts key secret)]
    (http/request {:url              url
                   :method           :post
                   :insecure?        true
                   :throw-exceptions false
                   :headers          {"X-Sentry-Auth" header "User-Agent" client-name}
                   :body             (json/generate-string event-info)})))

(defn capture [packet-info event-info]
  "Send a message to a Sentry server.
  event-info is a map that should contain a :message key and optional
  keys found at http://sentry.readthedocs.org/en/latest/developer/client/index.html#building-the-json-packet"
  (send-event
    packet-info
    (merge
      {:level       "error"
       :platform    "clojure"
       :server_name @hostname
       :timestamp   (ft/unparse (ft/formatters :date-hour-minute-second) (t/now))
       :event_id    (generate-uuid)}
      event-info)))

(defn- add-info [event-map iface info-fn req]
  (if info-fn
    (assoc event-map iface (info-fn req))
    event-map))

(defn -capture-error [{:keys [packet-info extra namespaces capture? http-info user-info]} req ^Throwable e]
  (when (and capture? (capture? e))
    (future
      (capture packet-info
               (-> (merge extra {:message (.getMessage e)})
                   (add-info "sentry.interfaces.Http" http-info req)
                   (add-info "sentry.interfaces.User" user-info req)
                   (add-stacktrace e namespaces))))))

(defn- build-url [{port :server-port :keys [scheme server-name uri]}]
  (str (when scheme (name scheme) "://") server-name
       (when (and port (not= 80 port))
         (str ":" port))
       uri))

(defn- http-info [req]
  {:url          (build-url req)
   :method       (:method req)
   :headers      (:headers req {})
   :query_string (:query-string req "")
   :data         (:params req {})
   :cookies      (:cookies req)
   :env          {:session (:session req {})}})

(defn parse-dsn [dsn]
  (let [[proto-auth url] (str/split dsn #"@")
        [protocol auth] (str/split proto-auth #"://")
        [key secret] (str/split auth #":")]
    {:key        key
     :secret     secret
     :uri        (format "%s://%s" protocol (str/join "/" (butlast (str/split url #"/"))))
     :project-id (Integer/parseInt (last (str/split url #"/")))}))

(defn -normalize [{:keys [dsn enabled? ignore?] :as config}]
  (let [enabled? (if (some? enabled?) enabled? (seq dsn))
        capture? (if (and enabled? ignore?)
                   (comp not ignore?)
                   (constantly enabled?))]
    (when enabled?
      (assoc config
        :packet-info (parse-dsn dsn)
        :capture? capture?
        :http-info (:http-info config http-info)))))

(def ^:private normalize (memoize -normalize))

(defn- extract-config [config req]
  (or (when (vector? config)
        (get-in req config))
      (when (ifn? config)
        (config req))
      (when (and (map? config) (contains? config :dsn))
        config)
      @fallback))

(defn capture-error
  ([e]
   (capture-error nil nil e))
  ([config-or-req e]
   (capture-error config-or-req config-or-req e))
  ([config req e]
   (#'-capture-error (normalize (extract-config config req)) req e)))
