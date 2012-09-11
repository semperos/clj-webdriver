;; Tests for RemoteWebDriver server and client (driver) code
(ns clj-webdriver.test.remote
  (:use clojure.test
        [clj-webdriver.core :only [quit to]]
        [clj-webdriver.test.config :only [test-base-url]]
        [clj-webdriver.test.util :only [start-server]]
        [clj-webdriver.test.common :only [run-common-tests]]
        [clj-webdriver.remote.server :only [new-remote-session stop]]))

(def server (atom nil))
(def driver (atom nil))

;; Fixtures
(defn start-session-fixture
  [f]
  (let [[this-server this-driver] (new-remote-session {:port 3003} {:browser :firefox})]
    (reset! server this-server)
    (reset! driver this-driver)))

(defn reset-browser-fixture
  [f]
  (to @driver test-base-url)
  (f))

(defn quit-fixture
  [f]
  (f)
  (quit @driver)
  (stop @server))

(use-fixtures :once start-server start-session-fixture quit-fixture)
(use-fixtures :each reset-browser-fixture)

;; RUN TESTS HERE
(deftest test-common-features-for-firefox-via-remote-server
  (run-common-tests @driver))
