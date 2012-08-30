term(ns clj-webdriver.remote.driver
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

(defprotocol IDesiredCapabilities
  "Way to interact with DesiredCapabilities settings"
  (browser-name [driver] "Get browser name of remote `driver`")
  (browser-name! [driver new-name] "Set browser name of remote `driver`")
  (capability [driver cap-name] "Get capability by name as String of remote `driver`")
  (capability! [driver cap-name cap-value] "Given a `k` key and `v` value compatible with any of the arities of `setCapability()`, set the value of the given capability for the given remote `driver`")
  (platform [driver] "Get platform of remote `driver`")
  (platform! [driver platform-name] "Given the name of a platform as either a keyword or string (case-insensitive), set the platform of the remote `driver` accordingly")
  (version [driver] "Get version of remote `driver`")
  (version! [driver version-string] "Set the version of the remote `driver`")
  (javascript [driver] "Get boolean value if javascript is enabled or not")
  (javascript! [driver enabled] "Set boolean value to enable or disable javascript"))

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
      (.setVersion caps new-version)))

  (javascript? [driver]
    (let [caps (capabilities driver)]
      (.isJavascriptEnabled caps)))

  (javascript! [driver enabled]
    (let [caps (capabilities driver)]
      (.setJavascriptEnabled caps enabled))))
