(defproject net.tbt-post/sentry-tiny "0.1.4-devel"
  :description "Tiny Sentry Interface for Clojure"
  :url "https://github.com/source-c/sentry-tiny"
  :license {:name "Eclipse Public License"
            :url  "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [cheshire "5.8.0"]
                 [http-kit "2.2.0"]
                 [clj-time "0.14.2"]
                 [danlentz/clj-uuid "0.1.7"]]
  :plugins [[lein-ancient "0.6.15"]]
  :profiles {:uberjar {:aot :all :jvm-opts ~(let [version (System/getProperty "java.version")
                                                  [major _ _] (clojure.string/split version #"\.")]
                                              (if (>= (Integer. major) 9) ;; FIXME: drop this tricky hack/hacky trick
                                                ["--add-modules" "java.xml.bind"]
                                                []))}})
