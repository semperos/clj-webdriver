;; Tests for JsonWireProtocol (IWire) support
(ns webdriver.wire-test
  (:require [clojure.test :refer :all]
            [webdriver.core :refer [quit to]]
            [webdriver.test.helpers :refer :all]
            [webdriver.remote.server :refer [new-remote-session stop]]
            [webdriver.remote.driver :refer [session-id]]
            [webdriver.wire :refer [execute]]))

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
    (is (= base-url
           (get-in resp [:body :value])))))
