(defproject net.info9/clj-webdriver "0.7.5"
  :description "Clojure API for Selenium-WebDriver"
  :url "https://github.com/semperos/clj-webdriver"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :min-lein-version "2.0.0"
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [org.clojure/core.cache "0.6.4"]
                 [org.clojure/tools.logging "0.3.1"]
                 [org.clojure/tools.namespace "0.2.10"]
                 [clj-http "2.0.0"]
                 [cheshire "5.5.0"]
                 [org.mortbay.jetty/servlet-api-2.5 "6.1.14"]
                 [org.eclipse.jetty.websocket/websocket-client "9.2.10.v20150310"]
                 [com.google.code.gson/gson "2.3.1"]
                 [org.seleniumhq.selenium/selenium-server "2.47.1"
                  :exclusions [org.eclipse.jetty.websocket/websocket-client]]
                 [org.seleniumhq.selenium/selenium-java "2.47.1"
                  :exclusions [org.eclipse.jetty.websocket/websocket-client]]
                 [org.seleniumhq.selenium/selenium-remote-driver "2.47.1"]
                 [com.github.detro/phantomjsdriver "1.2.0" :exclusions
                  [org.seleniumhq.selenium/selenium-java
                   org.seleniumhq.selenium/selenium-server
                   org.seleniumhq.selenium/selenium-remote-driver]]]
  :profiles {:dev
             {:dependencies
              [[criterium "0.4.3"]
               [codox "0.8.13"]
               [clj-time "0.10.0"]
               [org.clojure/clojurescript "1.7.28"]
               [marginalia "0.8.0"
                :exclusions
                [org.clojure/clojurescript]]
               [ring "1.4.0"]
               [ring-http-basic-auth "0.0.2"]
               [enlive "1.1.6"]
               [net.cgrand/moustache "1.1.0" :exclusions
                [org.clojure/clojure
                 ring/ring-core
                 org.mortbay.jetty/servlet-api-2.5]]]}}
  :aot [#"clj-webdriver\.ext\.*"]
  :scm {:url "git@github.com:tmarble/clj-webdriver.git"}
  :pom-addition [:developers [:developer [:name "Tom Marble"]]])
