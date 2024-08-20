(defproject io.logicblocks/aws.lambda "0.0.1-RC11"
  :description "A library for building Clojure programs using AWS Lambda."
  :url "https://github.com/logicblocks/aws.lambda"

  :license {:name "The MIT License"
            :url  "https://opensource.org/licenses/MIT"}

  :plugins
  [[lein-cloverage "1.2.4"]
   [lein-shell "0.5.0"]
   [lein-ancient "0.7.0"]
   [lein-changelog "0.3.2"]
   [lein-cprint "1.3.3"]
   [lein-eftest "0.6.0"]
   [lein-codox "0.10.8"]
   [lein-cljfmt "0.9.2"]
   [lein-kibit "0.1.8"]
   [lein-bikeshed "0.5.2"]
   [jonase/eastwood "1.4.0"]]

  :dependencies
  [[io.logicblocks/cartus.core "0.1.19-RC7"]
   [io.logicblocks/cartus.null "0.1.19-RC7"]
   [io.logicblocks/jason "1.0.0"]

   [ring/ring-core "1.12.1"]
   [ring/ring-codec "1.2.0"]

   [camel-snake-kebab "0.4.3"]

   [tick "0.7.5"]

   [com.amazonaws/aws-lambda-java-core "1.2.3"]
   [com.amazonaws/aws-lambda-java-runtime-interface-client "2.5.0"]]

  :profiles
  {:shared
   ^{:pom-scope :test}
   {:dependencies
    [[org.clojure/clojure "1.11.3"]

     [nrepl "1.1.2"]

     [org.clojure/tools.namespace "1.5.0"]
     [org.clojure/tools.trace "0.8.0"]
     [org.clojure/test.check "1.1.1"]

     [io.logicblocks/cartus.test "0.1.19-RC7"]

     [faker "0.3.2"]

     [eftest "0.6.0"]]}

   :dev
   [:shared {:aot          [aws.lambda.adapters.handlers-test
                            aws.lambda.adapters.api-gateway.handlers-test]
             :source-paths ["dev"]
             :eftest       {:multithread? false}}]

   :test
   [:shared {:aot    [aws.lambda.adapters.handlers-test
                      aws.lambda.adapters.api-gateway.handlers-test]
             :eftest {:multithread? false}}]

   :prerelease
   {:release-tasks
    [["shell" "git" "diff" "--exit-code"]
     ["change" "version" "leiningen.release/bump-version" "rc"]
     ["change" "version" "leiningen.release/bump-version" "release"]
     ["vcs" "commit" "Pre-release version %s [skip ci]"]
     ["vcs" "tag"]
     ["deploy"]]}

   :release
   {:release-tasks
    [["shell" "git" "diff" "--exit-code"]
     ["change" "version" "leiningen.release/bump-version" "release"]
     ["codox"]
     ["changelog" "release"]
     ["shell" "sed" "-E" "-i.bak" "s/\"[0-9]+\\.[0-9]+\\.[0-9]+\"/\"${:version}\"/g" "README.md"]
     ["shell" "rm" "-f" "README.md.bak"]
     ["shell" "git" "add" "."]
     ["vcs" "commit" "Release version %s [skip ci]"]
     ["vcs" "tag"]
     ["deploy"]
     ["change" "version" "leiningen.release/bump-version" "patch"]
     ["change" "version" "leiningen.release/bump-version" "rc"]
     ["change" "version" "leiningen.release/bump-version" "release"]
     ["vcs" "commit" "Pre-release version %s [skip ci]"]
     ["vcs" "tag"]
     ["vcs" "push"]]}}

  :cloverage
  {:ns-exclude-regex [#"^user"]}

  :codox
  {:namespaces  [#"^aws\.lambda\."]
   :metadata    {:doc/format :markdown}
   :output-path "docs"
   :doc-paths   ["docs"]
   :source-uri  "https://github.com/logicblocks/aws.lambda/blob/{version}/{filepath}#L{line}"}

  :cljfmt {:indents {#".*"     [[:inner 0]]
                     defrecord [[:block 1] [:inner 1]]
                     deftype   [[:block 1] [:inner 1]]}}

  :eastwood {:config-files ["config/linter.clj"]
             :exclude-linters [:reflection]}

  :deploy-repositories
  {"releases"  {:url "https://repo.clojars.org" :creds :gpg}
   "snapshots" {:url "https://repo.clojars.org" :creds :gpg}})
