(defproject clj-webdriver "0.7.1"
  :description "Clojure API for Selenium-WebDriver"
  :url "https://github.com/semperos/clj-webdriver"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :min-lein-version "2.0.0"
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/core.cache "0.5.0"]
                 [org.clojure/tools.logging "0.2.3"]
                 ;; Exclude these, giving preference to Selenium-WebDriver's
                 ;; dependence on them.
                 [clj-http "0.3.0" :exclusions [org.apache.httpcomponents/httpclient
                                                org.apache.httpcomponents/httpcore
                                                org.apache.httpcomponents/httpmime]]
                 [cheshire "2.1.0"]
                 [org.mortbay.jetty/jetty "6.1.25"]
                 [org.seleniumhq.selenium/selenium-server "2.35.0"]
                 [org.seleniumhq.selenium/selenium-java "2.35.0"]
                 [org.seleniumhq.selenium/selenium-remote-driver "2.35.0"]
                 [com.github.detro.ghostdriver/phantomjsdriver "1.0.4"
                  :exclusion [org.seleniumhq.selenium/selenium-java
                              org.seleniumhq.selenium/selenium-server
                              org.seleniumhq.selenium/selenium-remote-driver]]]
  :profiles {:dev
             {:dependencies
              [[criterium "0.2.0"]
               [codox "0.3.3"]
               [clj-time "0.4.4"]
               [marginalia "0.3.2"]
               [ring "1.0.2"]
               [ring-http-basic-auth "0.0.2"]
               [enlive "1.0.0"]
               [net.cgrand/moustache "1.0.0"]]}}
  :aot [#"clj-webdriver\.ext\.*"]
  :scm {:url "git@github.com:semperos/clj-webdriver.git"}
  :pom-addition [:developers [:developer [:name "Daniel Gregoire"]]])
