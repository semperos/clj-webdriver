(defproject net.info9/clj-webdriver "0.7.4"
  :description "Clojure API for Selenium-WebDriver"
  :url "https://github.com/semperos/clj-webdriver"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :min-lein-version "2.0.0"
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/core.cache "0.6.4"]
                 [org.clojure/tools.logging "0.3.1"]
                 [org.clojure/tools.namespace "0.2.10"]
                 [clj-http "1.1.2"]
                 [cheshire "5.4.0"]
                 [org.mortbay.jetty/servlet-api-2.5 "6.1.14"]
                 [org.eclipse.jetty/jetty-server "8.1.17.v20150415"]
                 [org.eclipse.jetty/jetty-webapp "8.1.17.v20150415"]
                 [org.eclipse.jetty/jetty-websocket "8.1.17.v20150415"]
                 [com.google.code.gson/gson "2.2.4"]
                 [org.seleniumhq.selenium/selenium-server "2.45.0"
                  :exclusions [commons-codec com.google.code.gson/gson]]
                 [org.seleniumhq.selenium/selenium-java "2.45.0"
                  :exclusions [commons-codec com.google.code.gson/gson]]
                 [org.seleniumhq.selenium/selenium-remote-driver "2.45.0"
                  :exclusions [com.google.code.gson/gson]]
                 [com.github.detro/phantomjsdriver "1.2.0" :exclusions
                  [org.seleniumhq.selenium/selenium-java
                   org.seleniumhq.selenium/selenium-server
                   org.seleniumhq.selenium/selenium-remote-driver]]]
  :profiles {:dev
             {:dependencies
              [[criterium "0.4.3"]
               [codox "0.8.12"]
               [clj-time "0.9.0"]
               [marginalia "0.8.0"
                :exclusions
                [org.clojure/tools.namespace
                 org.clojure/java.classpath]
                ]
               [ring "1.3.2"
                :exclusions
                [org.clojure/tools.namespace]]
               [ring-http-basic-auth "0.0.2"]
               [enlive "1.1.5"]
               [net.cgrand/moustache "1.1.0" :exclusions
                [org.clojure/clojure
                 ring/ring-core
                 org.mortbay.jetty/servlet-api-2.5]]]}}
  :aot [#"clj-webdriver\.ext\.*"]
  :scm {:url "git@github.com:tmarble/clj-webdriver.git"}
  :pom-addition [:developers [:developer [:name "Tom Marble"]]])
