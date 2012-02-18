(ns clj-webdriver.remote.driver
  (:use clj-webdriver.capabilities)
  (:import [org.openqa.selenium.remote
            DesiredCapabilities RemoteWebDriver
            HttpCommandExecutor]
           clj_webdriver.driver.Driver))

(defprotocol IRemoteWebDriver
  "RemoteWebDriver-specific functionality"
  (capabilities [driver] "Get capabilities of running `driver`")
  (command-executor [driver] "Get the CommandExecutor instance attached to this `driver`")
  (command-executor! [driver executor] "Set the CommandExecutor of the given `driver`")
  (session-id [driver] "Get the session id for the given `driver`")
  (session-id! [driver new-id] "Set the session id for the given `driver`"))

(extend-type Driver
  IRemoteWebDriver
  (capabilities [driver]
    (.getCapabilities (:webdriver driver)))
  
  (command-executor [driver]
    (.getCommandExecutor (:webdriver driver)))
  
  (command-executor! [driver executor]
    (.setCommandExecutor (:webdriver driver) executor))
  
  (session-id [driver]
    (str (.getSessionId (:webdriver driver))))
  
  (session-id! [driver new-id]
    (.setSessionId (:webdriver driver) new-id))
  
  IDesiredCapabilities
  (browser-name [driver]
    (let [caps (capabilities driver)]
      (.getBrowserName caps)))

  (browser-name! [driver new-name]
    (let [caps (capabilities driver)]
      (.setBrowserName caps new-name)))

  (capability [driver cap-name]
    (let [caps (capabilities driver)]
      (.getCapability caps (name cap-name))))

  (capability! [driver cap-name cap-value]
    (let [caps (capabilities driver)]
      (.setCapability caps cap-name cap-value)))

  (platform [driver]
    (let [caps (capabilities driver)]
      (.getPlatform caps)))

  (platform! [driver platform-name]
    (let [caps (capabilities driver)]
      (.setPlatform caps platform-name)))

  (version [driver]
    (let [caps (capabilities driver)]
      (.getVersion caps)))

  (version! [driver new-version]
    (let [caps (capabilities driver)]
      (.setVersion caps new-version))))