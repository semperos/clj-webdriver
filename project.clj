(defproject clj-webdriver "0.2.9"
  :description "Clojure wrapper around Selenium-WebDriver library"
  :dependencies [[org.clojure/clojure "1.3.0-beta1"]
                 [org.seleniumhq.selenium/selenium-server "2.3.0"]]
  :dev-dependencies [[ring "0.3.7"]
                     [enlive "1.0.0"]
                     [net.cgrand/moustache "1.0.0"]]
  :repositories {"selenium-repository" "http://selenium.googlecode.com/svn/repository/",
                 "java-dot-net" "http://download.java.net/maven/2"})