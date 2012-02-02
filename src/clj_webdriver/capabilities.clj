;; DesiredCapabilities support
(ns clj-webdriver.capabilities)

(defprotocol IDesiredCapabilities
  "Way to interact with DesiredCapabilities settings"
  (browser-name [driver] "Get browser name of remote `driver`")
  (browser-name! [driver new-name] "Set browser name of remote `driver`")
  (capability [driver cap-name] "Get capability by name as String of remote `driver`")
  (capability! [driver cap-name cap-value] "Given a `k` key and `v` value compatible with any of the arities of `setCapability()`, set the value of the given capability for the given remote `driver`")
  (platform [driver] "Get platform of remote `driver`")
  (platform! [driver platform-name] "Given the name of a platform as either a keyword or string (case-insensitive), set the platform of the remote `driver` accordingly")
  (version [driver] "Get version of remote `driver`")
  (version! [driver version-string] "Set the version of the remote `driver`"))

(defrecord Capabilities []
  IDesiredCapabilities
  (browser-name [caps]
    (.getBrowserName caps))

  (browser-name! [caps new-name]
    (.setBrowserName caps new-name))

  (capability [caps cap-name]
    (.getCapability caps (name cap-name)))

  (capability! [caps cap-name cap-value]
    (.setCapability caps cap-name cap-value))

  (platform [caps]
    (.getPlatform caps))

  (platform! [caps platform-name]
    (.setPlatform caps platform-name))

  (version [caps]
    (.getVersion caps))

  (version! [caps new-version]
    (.setVersion caps new-version)))
