{:dev      {:plugins             [[lein-ancient "0.6.15"]]
            :deploy-repositories [["private-jars" "http://10.10.3.4:9180/repo"]]}
 :provided {:dependencies [[org.clojure/clojure "1.9.0"]]}
 :uberjar  {:aot :all :jvm-opts #=(eval
                                    (concat ["-Xmx1G"]
                                      (let [version (System/getProperty "java.version")
                                            [major _ _] (clojure.string/split version #"\.")]
                                        (if (>= (Integer. major) 9)
                                          ["--add-modules" "java.xml.bind"]
                                          []))))}}