(defproject net.tbt-post/sentry-tiny (-> "VERSION" slurp .trim)
  :description "Tiny Sentry Interface for Clojure"
  :url "https://github.com/source-c/sentry-tiny"
  :license {:name "Apache License 2.0"
            :url  "http://www.apache.org/licenses/LICENSE-2.0"}
  :dependencies [[cheshire "5.10.1"]
                 [http-kit "2.5.3"]])
