(defproject clj-webdriver "0.2.14"
  :description "Clojure wrapper around Selenium-WebDriver library"
  :dependencies [[org.clojure/clojure "1.3.0-RC0"]
                 [org.seleniumhq.selenium/selenium-server "2.5.0"]]
  :dev-dependencies [[marginalia "0.3.2"]
                     [ring "0.3.7"]
                     [enlive "1.0.0"]
                     [net.cgrand/moustache "1.0.0"]]
  :repositories {"selenium-repository" "http://selenium.googlecode.com/svn/repository/",
                 "java-dot-net" "http://download.java.net/maven/2"})
