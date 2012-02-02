(ns clj-webdriver.remote
  (:use [clojure.java.io :only [as-url]]
        [clj-webdriver.driver :only [init-driver]]
        [clj-webdriver.core :only [get-url]])
  ;; Server dependencies
  (:import [org.mortbay.jetty Connector Server]
           org.mortbay.jetty.nio.SelectChannelConnector
           org.mortbay.jetty.security.SslSocketConnector
           org.mortbay.jetty.webapp.WebAppContext
           javax.servlet.Servlet
           org.openqa.selenium.remote.server.DriverServlet)
  ;; Client dependencies
  (:import [org.openqa.selenium.remote
            DesiredCapabilities RemoteWebDriver
            HttpCommandExecutor]
           clj_webdriver.driver.Driver))

(defn start-remote-server
  "Start a RemoteWebDriver server programmatically. This can also be accomplished using the standalone jar at the command-line."
  ([] (start-remote-server 3001 "/wd/*"))
  ([port] (start-remote-server port "/wd/*"))
  ([port path-spec]
     (let [server (Server.)
           context (WebAppContext.)
           connector (doto (SelectChannelConnector.)
                       (.setPort port))]
       (.setContextPath context "")
       (.setWar context ".")
       (.addHandler server context)
       (.addServlet context DriverServlet path-spec)
       (.addConnector server connector)
       (.start server)
       server)))

(defn stop-remote-server
  "Stop the RemoteWebDriver server."
  [server]
  (.stop server))

(defn new-remote-webdriver*
  [wd-url browser-spec]
  (let [capabilities {:android (DesiredCapabilities/android)
                      :chrome (DesiredCapabilities/chrome)
                      :firefox (DesiredCapabilities/firefox)
                      :htmlunit (DesiredCapabilities/htmlUnit)
                      :ie (DesiredCapabilities/internetExplorer)
                      :ipad (DesiredCapabilities/ipad)
                      :iphone (DesiredCapabilities/iphone)
                      :opera (DesiredCapabilities/opera)}
        {:keys [browser profile] :or {browser :firefox
                                      profile nil}} browser-spec]
    (if-not profile
      (RemoteWebDriver. (HttpCommandExecutor. (as-url wd-url))
                        (get capabilities browser)))))

(defn new-remote-driver
  [wd-url browser-spec]
  (let [{:keys [browser profile cache-spec] :or {browser :firefox
                                                 profile nil
                                                 cache-spec {}}} browser-spec]
    (init-driver {:webdriver (new-remote-webdriver* wd-url
                                                    {:browser browser
                                                     :profile profile})
                  :cache-spec cache-spec})))

(defn start-remote-driver
  "Shortcut to instantiate a driver, navigate to a URL, and return the driver for further use"
  ([wd-url browser-spec url]
     (let [driver (new-remote-driver wd-url browser-spec)]
       (get-url driver url)
       driver)))

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