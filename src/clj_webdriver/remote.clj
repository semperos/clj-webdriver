(ns clj-webdriver.remote
  (:use [clojure.java.io :only [as-url]]
        [clj-webdriver.driver :only [init-driver]])
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
            HttpCommandExecutor]))

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

(defprotocol IDesiredCapabilities
  "Way to interact with DesiredCapabilities settings"
  (browser-name [driver] "Get browser name of remote `driver`")
  (browser-name! [driver new-name] "Set browser name of remote `driver`")
  (capability [driver] "Get capability by name as String of remote `driver`")
  (capability! [driver k v] "Given a `k` key and `v` value compatible with any of the arities of `setCapability()`, set the value of the given capability for the given remote `driver`")
  (platform [driver] "Get platform of remote `driver`")
  (platform! [driver platform-name] "Given the name of a platform as either a keyword or string (case-insensitive), set the platform of the remote `driver` accordingly")
  (version [driver] "Get version of remote `driver`")
  (version! [driver version-string] "Set the version of the remote `driver`"))

(defprotocol IRemoteWebDriver
  "RemoteWebDriver-specific functionality"
  (execute [driver command & options] "Execute the given command against the running browser instance. Warning: low-level.")
  (capabilities [driver] "Get capabilities of running `driver`")
  (command-executor [driver] "Get the CommandExecutor instance attached to this `driver`")
  (command-executor! [driver executor] "Set the CommandExecutor of the given `driver`")
  (session-id [driver] "Get the session id for the given `driver`")
  (session-id! [driver new-id] "Set the session id for the given `driver`"))
