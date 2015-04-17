(defproject net.info9/clj-webdriver "0.7.3"
  :description "Clojure API for Selenium-WebDriver"
  :url "https://github.com/semperos/clj-webdriver"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :min-lein-version "2.0.0"
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/core.cache "0.6.4"]
                 [org.clojure/tools.logging "0.3.1"]
                 ;; Exclude these, giving preference to Selenium-WebDriver's
                 ;; dependence on them.
                 [clj-http "1.1.0" :exclusions
                  [org.apache.httpcomponents/httpclient
                   org.apache.httpcomponents/httpcore
                   org.apache.httpcomponents/httpmime]]
                 [cheshire "5.4.0"]
                 [org.mortbay.jetty/servlet-api-2.5 "6.1.14"]
                 [org.eclipse.jetty/jetty-server "8.1.16.v20140903"]
                 [org.eclipse.jetty/jetty-webapp "8.1.16.v20140903"]
                 [org.eclipse.jetty/jetty-websocket "8.1.16.v20140903"]
                 [org.seleniumhq.selenium/selenium-server "2.45.0"]
                 [org.seleniumhq.selenium/selenium-java "2.45.0"]
                 [org.seleniumhq.selenium/selenium-remote-driver "2.45.0"]
                 [com.github.detro/phantomjsdriver "1.2.0" :exclusions
                  [org.seleniumhq.selenium/selenium-java
                   org.seleniumhq.selenium/selenium-server
                   org.seleniumhq.selenium/selenium-remote-driver]]]
  :profiles {:dev
             {:dependencies
              [[criterium "0.4.3"]
               [codox "0.8.11"]
               [clj-time "0.9.0"]
               [marginalia "0.8.0"]
               [ring "1.3.2" :exclusions
                [org.clojure/tools.namespace
                 org.clojure/java.classpath]]
               [ring-http-basic-auth "0.0.2"]
               [enlive "1.1.5"]
               [net.cgrand/moustache "1.1.0" :exclusions
                [org.clojure/clojure
                 ring/ring-core
                 org.mortbay.jetty/servlet-api-2.5]]]}}
  :aot [#"clj-webdriver\.ext\.*"]
  :scm {:url "git@github.com:tmarble/clj-webdriver.git"}
  :pom-addition [:developers [:developer [:name "Tom Marble"]]])
