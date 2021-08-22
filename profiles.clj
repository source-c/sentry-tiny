{:dev      {:plugins []}
 :provided {:dependencies [[org.clojure/clojure "1.10.3"]]
            :java-source-paths #{"java"}
            :resource-paths    ["resources"]

            :javac-options     ["-source" "9" "-target" "9" "-g:none"]

            :jar-exclusions    [#"\.java"]}
 :jar      {:aot :all}}
