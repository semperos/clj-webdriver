(ns clj-webdriver.remote.driver
  (:import [org.openqa.selenium.remote
            DesiredCapabilities
            RemoteWebDriver]
           clj_webdriver.driver.Driver))

(defprotocol IRemoteWebDriver
  "RemoteWebDriver-specific functionality"
  (capabilities [driver] "Get capabilities of running `driver`")
  (command-executor [driver] "Get the CommandExecutor instance attached to this `driver`")
  (command-executor! [driver executor] "Set the CommandExecutor of the given `driver`")
  (session-id [driver] "Get the session id for the given `driver`")
  (session-id! [driver new-id] "Set the session id for the given `driver`"))


