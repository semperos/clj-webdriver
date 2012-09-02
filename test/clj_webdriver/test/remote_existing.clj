;; Tests for RemoteWebDriver server and client (driver) code using manually-started Grid hub
(ns clj-webdriver.test.remote-existing
  (:use clojure.test
        [clj-webdriver.core :only [quit to]]
        [clj-webdriver.test.config :only [test-base-url]]
        [clj-webdriver.test.util :only [start-server]]
        [clj-webdriver.test.common :only [run-common-tests]]
        [clj-webdriver.remote.server :only [new-remote-session stop]]))

;; Utilities
(defn hub-host
  []
  (get (System/getenv) "WEBDRIVER_HUB_HOST" "127.0.0.1"))

(defn hub-port
  []
  ;; API default is 4444, so for testing we use 3333
  ;; see scripts/grid-hub and scripts/grid-node
  (int (get (System/getenv) "WEBDRIVER_HUB_PORT" 3333)))

(use-fixtures :once start-server)

;; RUN TESTS HERE
(deftest test-suite-with-remote-driver-attached-to-manually-started
  (let [[server driver] (new-remote-session {:port (hub-port)
                                             :host (hub-host)
                                             :existing true}
                                            {:browser :firefox})]
    (to driver test-base-url)
    (run-common-tests driver)
    (quit driver)))

(deftest test-suite-with-remote-driver-attached-to-manually-started-with-capabilities
  (let [capabilities {"browserName" "firefox", "seleniumProtocol" "Selenium"}
       [server driver] (new-remote-session {:port (hub-port)
                                            :host (hub-host)
                                            :existing true}
                                           {:capabilities capabilities})]
    (to driver test-base-url)
    (run-common-tests driver)
    (quit driver)))