(defproject clj-webdriver "0.7.0-SNAPSHOT"
  :description "Clojure API for Selenium-WebDriver"
  :url "https://github.com/semperos/clj-webdriver"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :min-lein-version "2.0.0"
  :dependencies [[org.clojure/core.cache "0.5.0"]
                 [org.clojure/tools.logging "0.2.3"]
                 [clj-http "2.0.0"]
                 [cheshire "5.5.0"]
                 [org.mortbay.jetty/jetty "6.1.25"]]
  :profiles {:dev {:dependencies [[org.clojure/clojure "1.6.0"]
                                  [org.slf4j/slf4j-log4j12 "1.7.5"]
                                  [com.stuartsierra/component "0.2.3"]
                                  [ring/ring-jetty-adapter "1.4.0"]
                                  [enlive "1.0.0" :exclusions [org.clojure/clojure]]
                                  [net.cgrand/moustache "1.0.0" :exclusions [org.clojure/clojure ring/ring-core]]
                                  [org.seleniumhq.selenium/selenium-server "2.47.1"]
                                  ;; [org.seleniumhq.selenium/selenium-java "2.43.0"]
                                  [org.seleniumhq.selenium/selenium-remote-driver "2.47.1"]
                                  [com.github.detro/phantomjsdriver "1.2.0"
                                   :exclusion [org.seleniumhq.selenium/selenium-java
                                               org.seleniumhq.selenium/selenium-server
                                               org.seleniumhq.selenium/selenium-remote-driver]]]}}
  :aot [#"clj-webdriver\.ext\.*"]
  :scm {:url "git@github.com:semperos/clj-webdriver.git"}
  :pom-addition [:developers [:developer [:name "Daniel Gregoire"]]])
