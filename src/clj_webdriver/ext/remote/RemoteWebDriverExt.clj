(ns ^{:doc "Custom implementation of RemoteWebDriver, adding support for screenshots."}
  clj-webdriver.ext.remote.RemoteWebDriverExt
  (:import [org.openqa.selenium.remote DriverCommand RemoteWebDriver])
  (:gen-class
   ;; :name demo.interop.Instance
   :main false
   :extends org.openqa.selenium.remote.RemoteWebDriver
   :implements [org.openqa.selenium.TakesScreenshot]))

(defn -getScreenshotAs
  [this target]
    ;;   String base64 = (String) execute(DriverCommand.SCREENSHOT).getValue();
    ;; // ... and convert it.
  ;; return target.convertFromBase64Png(base64);
  (let [base64 (->> DriverCommand/SCREENSHOT
                    (. this execute)
                    .getValue
                    str)]
    (. target convertFromBase64Png base64)))
    