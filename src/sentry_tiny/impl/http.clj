(ns sentry-tiny.impl.http
  (:require [clojure.string :as str])
  (:import (java.net.http HttpRequest
                          HttpResponse
                          HttpClient
                          HttpClient$Version
                          HttpRequest$Builder
                          HttpRequest$BodyPublishers
                          HttpResponse$BodyHandlers)
           (java.util.function Supplier)
           (java.time Duration)
           (java.net URI)
           (java.io InputStream)))

(def ^:private ^HttpClient default-client
  (delay (HttpClient/newHttpClient)))

(def ^:private bh->string (HttpResponse$BodyHandlers/ofString))
(def ^:private bh->istream (HttpResponse$BodyHandlers/ofInputStream))
(def ^:private bh->bytes (HttpResponse$BodyHandlers/ofByteArray))

(def ^:private convert-headers-xf
  (mapcat
    (fn [[k v :as p]]
      (if (sequential? v)
        (interleave (repeat k) v)
        p))))

(def ^:private bytes-class
  (Class/forName "[B"))

(defn- convert-body-handler [mode]
  (case mode
    nil bh->string
    :string bh->string
    :input-stream bh->istream
    :byte-array bh->bytes))

(defn- version-enum->version-keyword [^HttpClient$Version version]
  (case (.name version)
    "HTTP_1_1" :http1.1
    "HTTP_2" :http2))

(defn- version-keyword->version-enum [version]
  (case version
    :http1.1 HttpClient$Version/HTTP_1_1
    :http2 HttpClient$Version/HTTP_2))

(defn- method-keyword->str [method]
  (str/upper-case (name method)))

(defn- convert-timeout [t]
  (if (integer? t)
    (Duration/ofMillis t)
    t))

(defn- input-stream-supplier [s]
  (reify Supplier
    (get [this] s)))

(defn- convert-body-publisher [body]
  (cond
    (nil? body)
    (HttpRequest$BodyPublishers/noBody)

    (string? body)
    (HttpRequest$BodyPublishers/ofString body)

    (instance? InputStream body)
    (HttpRequest$BodyPublishers/ofInputStream (input-stream-supplier body))

    (instance? bytes-class body)
    (HttpRequest$BodyPublishers/ofByteArray body)))

(defn request-builder ^HttpRequest$Builder [opts]
  (let [{:keys [expect-continue?
                headers
                method
                timeout
                uri
                version
                body]} opts]
    (cond-> (HttpRequest/newBuilder)
            (some? expect-continue?) (.expectContinue expect-continue?)
            (seq headers) (.headers (into-array String (eduction convert-headers-xf headers)))
            method (.method (method-keyword->str method) (convert-body-publisher body))
            timeout (.timeout (convert-timeout timeout))
            uri (.uri (URI/create uri))
            version (.version (version-keyword->version-enum version)))))

(defn- build-request
  (^HttpRequest [] (.build (request-builder {})))
  (^HttpRequest [req-map] (.build (request-builder req-map))))

(defn- response->map [^HttpResponse resp]
  {:status  (.statusCode resp)
   :body    (.body resp)
   :version (-> resp .version version-enum->version-keyword)
   :headers (into {}
                  (map (fn [[k v]] [k (if (> (count v) 1) (vec v) (first v))]))
                  (.map (.headers resp)))})

(defn- convert-request [req]
  (cond
    (map? req) (build-request req)
    (string? req) (build-request {:uri req})
    (instance? HttpRequest req) req))

(defn send
  ([req]
   (send req {}))
  ([req {:keys [as client raw?] :as opts}]
   (let [^HttpClient client (or client @default-client)
         req' (convert-request req)
         resp (.send client req' (convert-body-handler as))]
     (if raw? resp (response->map resp)))))
