(ns core
  (:require [clojure.test :refer :all]
            [sentry-tiny.impl.utils :refer :all]))

(deftest build-url-simple
  (binding [*ns* 'sentry-tiny.core]
    (is (= "https://some.host:1234/path"
           (build-url {:scheme      :https
                       :server-name "some.host"
                       :server-port 1234
                       :uri         "/path"})))
    (are [url scheme port]
      (= url (build-url {:scheme scheme :server-port port}))
      "http://" :http 80
      "http://:1" :http 1
      "https://" :https 443
      "https://:1" :https 1
      "unknown://" :unknown nil
      "unknown://:1" :unknown 1)))
