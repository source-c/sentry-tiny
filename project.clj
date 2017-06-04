(defproject local.1st/clj-helpers-sentry "0.1.1"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [cheshire "5.7.1"]
                 [http-kit "2.2.0"]
                 [danlentz/clj-uuid "0.1.7"]]
  :repositories [["private-jars" "http://10.10.3.4:9180/repo"]]
  :deploy-repositories [["private-jars" "http://10.10.3.4:9180/repo"]])
