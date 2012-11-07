(defproject clj-webdriver "0.6.0-beta2"
  :description "Clojure API for Selenium-WebDriver"
  :url "https://github.com/semperos/clj-webdriver"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :min-lein-version "2.0.0"
  :dependencies [[org.clojure/clojure "1.4.0"]
                 [org.clojure/core.cache "0.5.0"]
                 [org.clojure/tools.logging "0.2.3"]
                 [clj-http "0.3.0"]
                 [cheshire "2.1.0"]
                 [org.mortbay.jetty/jetty "6.1.25"]
                 [org.seleniumhq.selenium/selenium-server "2.26.0"]]
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
