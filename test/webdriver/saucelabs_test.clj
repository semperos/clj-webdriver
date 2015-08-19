(ns ^:saucelabs webdriver.saucelabs-test
  "Tests running on SauceLabs using 'Open Sauce' subscription"
  (:require [clojure.test :refer [deftest use-fixtures]]
            [webdriver.core :refer [quit to]]
            [webdriver.test.helpers :refer :all]
            [webdriver.test.common :refer [defcommontests]])
  (:import java.net.URL
           [java.util.logging Level]
           org.openqa.selenium.Platform
           [org.openqa.selenium.remote CapabilityType DesiredCapabilities RemoteWebDriver]))

(def server (atom nil))
(def driver (atom nil))

(defn restart-session
  [f]
  (when (not @driver)
    (let [{:keys [user token host port]} (:saucelabs system)
          caps (doto (DesiredCapabilities.)
                 (.setCapability CapabilityType/BROWSER_NAME "firefox")
                 (.setCapability CapabilityType/PLATFORM Platform/MAC)
                 (.setCapability "name" "clj-webdriver-test-suite"))
          url (str "http://" user ":" token "@" host ":" port "/wd/hub")
          wd (RemoteWebDriver. (URL. url) caps)
          session-id (str (.getSessionId wd))]
      (reset! driver wd)))
  (to @driver heroku-url)
  (f))

(defn quit-session
  [f]
  (f)
  (quit @driver))

(use-fixtures :once start-system! stop-system! quit-session)
(use-fixtures :each restart-session)

;; RUN TESTS HERE
(defcommontests "test-" heroku-url @driver)
