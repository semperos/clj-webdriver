;; Tests for RemoteWebDriver server and client (driver) code
(ns clj-webdriver.test.remote
  (:use clojure.test
        [clj-webdriver.core :only [quit to]]
        [clj-webdriver.test.config :only [test-base-url]]
        [clj-webdriver.test.util :only [start-server]]
        [clj-webdriver.test.common :only [run-common-tests]]
        [clj-webdriver.remote.server :only [new-remote-session stop]])
  (:require 
            [clj-webdriver.remote.driver :as rd]))

;; Non-standard port to avoid conflicts
(let [[this-server this-driver] (new-remote-session {:port 3003} {:browser :firefox})]
  (def server this-server)
  (def driver this-driver))

;; Fixtures
(defn reset-browser-fixture
  [f]
  (to driver test-base-url)
  (f))

(defn quit-fixture
  [f]
  (f)
  (quit driver)
  (stop server))

(use-fixtures :once start-server quit-fixture)
(use-fixtures :each reset-browser-fixture)

;; RUN TESTS HERE
(deftest test-common-features-for-firefox-via-remote-server
  (run-common-tests driver))