(ns clj-webdriver.remote.server
  (:use [clojure.java.io :only [as-url]]
        [clj-webdriver.driver :only [init-driver]]
        [clj-webdriver.core :only [get-url]])
  (:import [org.mortbay.jetty Connector Server]
           org.mortbay.jetty.nio.SelectChannelConnector
           org.mortbay.jetty.security.SslSocketConnector
           org.mortbay.jetty.webapp.WebAppContext
           javax.servlet.Servlet
           org.openqa.selenium.remote.server.DriverServlet
           [org.openqa.selenium.remote
            DesiredCapabilities RemoteWebDriver
            HttpCommandExecutor]))

(defprotocol IRemoteServer
  "Functions for managing a RemoteServer instance."
  (start [server] "Start the server. Will try to run stop if a bind exception occurs.")
  (stop [server] "Stop the server")
  (address [server] "Get address of the server")
  (new-remote-webdriver* [server browser-spec] "Internal: start a new instance of RemoteWebDriver, used by `new-remote-driver`.")
  (new-remote-driver [server browser-spec] "Instantiate a new RemoteDriver record.")
  (start-remote-driver [server browser-spec target-url] "Start a new RemoteDriver record and go to `target-url`."))

(defrecord RemoteServer [connection-params webdriver-server]
  IRemoteServer
  (stop [remote-server]
    (.stop (:webdriver-server remote-server)))
  
  (start [remote-server]
    (try
      (let [port (get-in remote-server [:connection-params :port])
            path-spec (get-in remote-server [:connection-params :path-spec])
            server (Server.)
            context (WebAppContext.)
            connector (doto (SelectChannelConnector.)
                        (.setPort port))]
        (.setContextPath context "")
        (.setWar context ".")
        (.addHandler server context)
        (.addServlet context DriverServlet path-spec)
        (.addConnector server connector)
        (.start server)
        server)
      (catch java.net.BindException _
        (stop remote-server)
        (start remote-server))))
  
  (address [remote-server]
    (str "http://"
         (get-in remote-server [:connection-params :host])
         ":"
         (get-in remote-server [:connection-params :port])
         (apply str (drop-last (get-in remote-server [:connection-params :path-spec])))))
  
  (new-remote-webdriver*
    [remote-server browser-spec]
    (let [wd-url (address remote-server)
          capabilities {:android (DesiredCapabilities/android)
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

  (new-remote-driver
    [remote-server browser-spec]
    (let [{:keys [browser profile cache-spec] :or {browser :firefox
                                                   profile nil
                                                   cache-spec {}}} browser-spec]
      (init-driver {:webdriver (new-remote-webdriver* remote-server
                                                      {:browser browser
                                                       :profile profile})
                    :cache-spec cache-spec})))

  (start-remote-driver
    [remote-server browser-spec url]
    (let [driver (new-remote-driver remote-server browser-spec)]
      (get-url driver url)
      driver)))

(defn init-remote-server
  "Initialize a new RemoteServer record, optionally starting the server automatically (enabled by default)."
  ([connection-params] (init-remote-server connection-params true))
  ([{:keys [host port path-spec]
     :as connection-params
     :or {host "127.0.0.1" port 3001 path-spec "/wd/*"}}
    start?]
     (let [server-record (RemoteServer. {:host host
                                         :port port
                                         :path-spec path-spec}
                                        nil)]
       (if start?
         (assoc server-record :webdriver-server (start server-record))
         server-record))))

(defn remote-server?
  [rs]
  (= (class rs) RemoteServer))

(defn new-remote-session
  "Start up a server, start up a driver, return both in that order. Pass a final falsey arg to prevent the server from being started for you."
  ([] (new-remote-session {}))
  ([connection-params] (new-remote-session connection-params {:browser :firefox}))
  ([connection-params browser-spec] (new-remote-session connection-params browser-spec true))
  ([connection-params browser-spec start?]
     (let [new-server (if (remote-server? connection-params)
                        connection-params
                        (init-remote-server connection-params start?))
           new-driver (new-remote-driver new-server browser-spec)]
       [new-server new-driver])))