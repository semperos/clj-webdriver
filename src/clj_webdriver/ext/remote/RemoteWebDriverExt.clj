(ns ^{:doc "Custom implementation of RemoteWebDriver, adding support for screenshots."}
  clj-webdriver.ext.remote.RemoteWebDriverExt
  (:import [org.openqa.selenium.remote DriverCommand RemoteWebDriver])
  (:gen-class
   :main false
   :extends org.openqa.selenium.remote.RemoteWebDriver
   :implements [org.openqa.selenium.TakesScreenshot]))

(defn -getScreenshotAs
  [this target]
  (let [base64 (->> DriverCommand/SCREENSHOT
                    (.execute this)
                    .getValue
                    str)]
    (.convertFromBase64Png target base64)))

