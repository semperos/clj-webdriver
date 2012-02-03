;; Tests for RemoteWebDriver server and client (driver) code
(ns clj-webdriver.test.remote
  (:use clojure.test
        [clj-webdriver.core :only [quit to]]
        [clj-webdriver.test.config :only [test-base-url]]
        [clj-webdriver.test.util :only [start-server]]
        [clj-webdriver.test.common :only [run-common-tests]]
        [clj-webdriver.remote-server :only [init-remote-server stop new-remote-driver]])
  (:require 
            [clj-webdriver.remote-driver :as rd]))

(def server (init-remote-server {:port 3003, :path-spec "/wd/*"}))

(def driver (new-remote-driver server {:browser :firefox}))

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