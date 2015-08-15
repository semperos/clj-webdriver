(ns webdriver.remote-test
  "Tests for RemoteWebDriver server and client (driver) code"
  (:require [clojure.test :refer :all]
            [clj-webdriver.core :refer [quit to]]
            [clj-webdriver.test.helpers :refer :all]
            [clj-webdriver.test.common :refer [defcommontests]]
            [clj-webdriver.remote.server :refer [new-remote-session stop]])
  (:import [java.util.logging Level]))

(def server (atom nil))
(def driver (atom nil))

;; Fixtures
(defn restart-session
  [f]
  (when (and (not @server) (not @driver))
    (let [[this-server this-driver] (new-remote-session {:port 3004} {:browser :firefox})]
      (reset! server this-server)
      (reset! driver this-driver)))
  (to @driver base-url)
  (f))

(defn quit-session
  [f]
  (f)
  (quit @driver)
  (stop @server))

(use-fixtures :once start-system! stop-system! quit-session)
(use-fixtures :each restart-session)

;; RUN TESTS HERE
(defcommontests "test-" @driver)
