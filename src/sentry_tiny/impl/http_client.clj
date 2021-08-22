(ns sentry-tiny.impl.http-client)

(declare http)
(declare request)

(defn load-http-kit []
  (Class/forName "org.httpkit.client.HttpClient")
  (load "/org/httpkit/client")
  (in-ns 'sentry-tiny.impl.http-client)
  (alter-var-root #'http (constantly 'org.httpkit.client))
  (alter-var-root #'request (constantly (resolve 'org.httpkit.client/request))))

(defn load-java-native []
  (in-ns 'sentry-tiny.impl.http-client)
  (load "http")
  (refer 'sentry-tiny.impl.http :only '[request])
  (alter-var-root #'http (constantly 'sentry-tiny.impl.http))
  (alter-var-root #'request (constantly (resolve 'sentry-tiny.impl.http/request)))
  (println "Meta:" (meta #'request)))

(def client-inited (promise))

(defn reflect-client []
  (try (load-http-kit)
       (catch Exception _
         println "http-kit not found"
         (load-java-native)))
  (deliver client-inited true)
  @client-inited)


