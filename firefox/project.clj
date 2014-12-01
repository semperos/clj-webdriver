(defproject clj-webdriver "0.7.0-SNAPSHOT"
  :description "Clojure API for Selenium-WebDriver, Firefox-only"
  :url "https://github.com/semperos/clj-webdriver"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :min-lein-version "2.0.0"
  :dependencies [[org.seleniumhq.selenium/selenium-firefox-driver "2.44.0"]]
  :scm {:url "git@github.com:semperos/clj-webdriver.git"}
  :pom-addition [:developers [:developer [:name "Daniel Gregoire"]]])
