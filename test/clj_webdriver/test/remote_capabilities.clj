(ns ^{:doc "Tests for RemoteWebDriver server and client (driver) code"}
  clj-webdriver.test.remote-capabilities
  (:require [clojure.test :refer :all]
            [clj-webdriver.core :refer [quit to]]
            [clj-webdriver.test.helpers :refer [base-url start-system! stop-system!]]
            [clj-webdriver.test.common :refer [run-common-tests]]
            [clj-webdriver.remote.server :refer [new-remote-session stop]])
  (:import org.openqa.selenium.remote.DesiredCapabilities))

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
(deftest test-common-features-for-firefox-via-remote-server-with-capabilities
  (run-common-tests @driver))

(deftest test-capabilities-in-driver-record
  (is (= (count (:capabilities @driver)) 4))
  (is (= (set (keys (:capabilities @driver))) #{:desired :desired-obj :actual :actual-obj}))
  (is (= (get-in @driver [:capabilities :desired]) {:browser-name "firefox"}))
  (is (> (count (get-in @driver [:capabilities :actual]))
         (count (get-in @driver [:capabilities :desired]))))
  (is (= (class (get-in @driver [:capabilities :desired-obj]))
         DesiredCapabilities))
  (is (= (class (get-in @driver [:capabilities :actual-obj]))
         DesiredCapabilities)))
