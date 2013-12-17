;; Tests for RemoteWebDriver server and client (driver) code using manually-started Grid hub
(ns clj-webdriver.test.remote-existing
  (:use clojure.test
        [clj-webdriver.core :only [quit to]]
        [clj-webdriver.test.config :only [base-url]]
        [clj-webdriver.test.util :only [start-server]]
        [clj-webdriver.test.common :only [run-common-tests]]
        [clj-webdriver.remote.server :only [new-remote-session stop]]
        [clj-webdriver.test.common :only [run-common-tests run-phantomjs-tests]])
  (:import [java.util.logging Level]))

(def BROWSERS [:firefox :phantomjs :chrome])
(def server (atom nil))
(def driver (atom nil))

;; Utilities
(defn hub-host
  []
  (get (System/getenv) "WEBDRIVER_HUB_HOST" "127.0.0.1"))

(defn hub-port
  []
  ;; API default is 4444, so for testing we use 3333
  ;; see scripts/grid-hub and scripts/grid-node
  (Integer/parseInt (get (System/getenv) "WEBDRIVER_HUB_PORT" 3333)))

;; Fixtures
(defn start-session-fixture
  [f]
  (let [[this-server this-driver] (new-remote-session {:port (hub-port)
                                                       :host (hub-host)
                                                       :existing true}
                                                      {:browser :firefox})]
    (-> this-driver :webdriver (.setLogLevel Level/OFF))
    (reset! server this-server)
    (reset! driver this-driver))
  (f))

(defn reset-browser-fixture
  [f]
  (if @driver (to @driver (base-url)))
  (f))

(defn quit-fixture
  [f]
  (f)
  (if @driver (quit @driver)))

(use-fixtures :once start-server quit-fixture)
;(use-fixtures :each reset-browser-fixture)

;; RUN TESTS HERE

(deftest test-remote-driver-attached-to-manually-started-grid-firefox
  (let  [[this-server this-driver] (new-remote-session {:port (hub-port)
                                                        :host (hub-host)
                                                        :existing true}
                                                      {:browser :firefox})]
    (-> this-driver :webdriver (.setLogLevel Level/OFF))
    (reset! server this-server)
    (reset! driver this-driver)
    (to @driver (base-url))
    (run-common-tests @driver)
    (quit @driver)))

(deftest test-remote-driver-attached-to-manually-started-grid-chrome
  (let  [[this-server this-driver] (new-remote-session {:port (hub-port)
                                                        :host (hub-host)
                                                        :existing true}
                                                      {:browser :chrome})]
    (-> this-driver :webdriver (.setLogLevel Level/OFF))
    (reset! server this-server)
    (reset! driver this-driver)
    (to @driver (base-url))
    (run-common-tests @driver)))

(deftest test-remote-driver-attached-to-manually-started-grid-phantomjs
  (let  [[this-server this-driver] (new-remote-session {:port (hub-port)
                                                        :host (hub-host)
                                                        :existing true}
                                                      {:browser :phantomjs})]
    (-> this-driver :webdriver (.setLogLevel Level/OFF))
    (reset! server this-server)
    (reset! driver this-driver)
    (to @driver (base-url))
    (run-phantomjs-tests @driver)
    (quit @driver)))

;; (deftest test-remote-driver-attached-to-manually-started-grid-with-capabilities
;;   (let [capabilities {"browserName" "firefox", "seleniumProtocol" "Selenium"}
;;        [server driver] (new-remote-session {:port (hub-port)
;;                                             :host (hub-host)
;;                                             :existing true}
;;                                            {:capabilities capabilities})]
;;     (run-common-tests driver)
;;     (quit driver)))
