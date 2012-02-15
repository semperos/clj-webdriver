(defproject clj-webdriver "0.5.1"
  :description "Clojure wrapper around Selenium-WebDriver library"
  :dependencies [[org.clojure/clojure "1.3.0"]
                 [clj-http "0.3.0"]
                 [cheshire "2.1.0"]
                 [org.mortbay.jetty/jetty "6.1.25"]
                 [org.seleniumhq.selenium/selenium-server "2.19.0"]
                 [com.opera/operadriver "0.9"]
                 [org.clojure/core.cache "0.5.0"]
                 [org.clojure/tools.logging "0.2.3"]]
  :dev-dependencies [[criterium "0.2.0"]
                     [codox "0.3.3"]
                     [marginalia "0.3.2"]
                     [ring "1.0.2"]
                     [enlive "1.0.0"]
                     [net.cgrand/moustache "1.0.0"]])
