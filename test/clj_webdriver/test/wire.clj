;; Tests for JsonWireProtocol (IWire) support
(ns clj-webdriver.test.wire
  (:require [clojure.test :refer :all]
            [clj-webdriver.core :refer [quit to]]
            [clj-webdriver.test.helpers :refer [base-url start-system! stop-system!]]
            [clj-webdriver.remote.server :refer [new-remote-session stop]]
            [clj-webdriver.remote.driver :refer [session-id]]
            [clj-webdriver.wire :refer [execute]]))

(def server (atom nil))
(def driver (atom nil))

;; Fixtures
(defn start-session-fixture
  [f]
  (let [[this-server this-driver] (new-remote-session {:port 3004} {:browser :firefox})]
    (reset! server this-server)
    (reset! driver this-driver))
  (f))

(defn reset-browser-fixture
  [f]
  (to @driver (base-url))
  (f))

(defn quit-fixture
  [f]
  (f)
  (quit @driver)
  (stop @server))

(use-fixtures :once start-system! stop-system! start-session-fixture quit-fixture)
(use-fixtures :each reset-browser-fixture)

;; RUN TESTS HERE
(deftest execute-status-should-return-successfully
  (let [resp (execute @server ["status"])]
    (is (= 200
           (:status resp)))
    (is (zero?
         (get-in resp [:body :status])))
    (is (= "org.openqa.selenium.remote.Response"
           (get-in resp [:body :class])))))

(deftest execute-url-should-return-successfully
  (let [sessid (session-id @driver)
        resp (execute @server ["session" sessid "url"])]
    (is (= (base-url)
           (get-in resp [:body :value])))))
