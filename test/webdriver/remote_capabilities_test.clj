(ns webdriver.remote-capabilities-test
  "Tests for RemoteWebDriver server and client (driver) code"
  (:require [clojure.test :refer :all]
            [webdriver.core :refer [quit to]]
            [webdriver.test.helpers :refer [base-url start-system! stop-system!]]
            [webdriver.test.common :refer [defcommontests]]
            [webdriver.remote.server :refer [new-remote-session stop]])
  (:import org.openqa.selenium.Platform
           org.openqa.selenium.remote.DesiredCapabilities))

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

(deftest test-capabilities-in-driver-record
  (is (= (count (:capabilities @driver)) 4))
  (is (= (set (keys (:capabilities @driver))) #{:desired :desired-obj :actual :actual-obj}))
  (is (every? (partial (get-in @driver [:capabilities :desired]) contains?)
              [:platform :browser :version] ))
  (is (= "firefox" (get-in @driver [:capabilities :desired :browser-name])))
  (is (> (count (get-in @driver [:capabilities :actual]))
         (count (get-in @driver [:capabilities :desired]))))
  (is (= (class (get-in @driver [:capabilities :desired-obj]))
         DesiredCapabilities))
  (is (= (class (get-in @driver [:capabilities :actual-obj]))
         DesiredCapabilities)))
