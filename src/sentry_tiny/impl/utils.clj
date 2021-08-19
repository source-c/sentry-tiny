(ns sentry-tiny.impl.utils)

(defn build-url
  "Reconstruct a URL from a ring request map, using the keys defined in
   https://github.com/ring-clojure/ring/wiki/Concepts"
  [{port :server-port :keys [scheme server-name uri]}]
  (str (when scheme (str (name scheme) "://")) server-name
       (when (and port (not= ({:http 80 :https 443} scheme) port))
         (str ":" port))
       uri))
