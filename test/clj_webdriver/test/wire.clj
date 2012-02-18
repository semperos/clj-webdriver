;; Tests for JsonWireProtocol (IWire) support
(ns clj-webdriver.test.wire
  (:use clojure.test
        [clj-webdriver.core :only [quit to]]
        [clj-webdriver.test.config :only [test-base-url]]
        [clj-webdriver.test.util :only [start-server]]
        [clj-webdriver.test.common :only [run-common-tests]]
        [clj-webdriver.remote.server :only [new-remote-session stop]]
        [clj-webdriver.remote.driver :only [session-id]]
        [clj-webdriver.wire :only [execute]])
  (:require 
            [clj-webdriver.remote.driver :as rd]))

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
(deftest execute-status-should-return-successfully
  (let [resp (execute server ["status"])]
    (is (= 200
           (:status resp)))
    (is (zero?
         (get-in resp [:body :status])))
    (is (= "org.openqa.selenium.remote.Response"
           (get-in resp [:body :class])))))

(deftest execute-url-should-return-successfully
  (let [sessid (session-id driver)
        resp (execute server ["session" sessid "url"])]
    (is (= test-base-url
           (get-in resp [:body :value])))))