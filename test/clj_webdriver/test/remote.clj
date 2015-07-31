(ns ^{:doc "Tests for RemoteWebDriver server and client (driver) code"}
  clj-webdriver.test.remote
  (:require [clojure.test :refer :all]
            [clj-webdriver.core :refer [quit to]]
            [clj-webdriver.test.helpers :refer [base-url start-system! stop-system!]]
            [clj-webdriver.test.common :refer [run-common-tests]]
            [clj-webdriver.remote.server :refer [new-remote-session stop]])
  (:import [java.util.logging Level]))

(def server (atom nil))
(def driver (atom nil))

;; Fixtures
(defn start-session-fixture
  [f]
  (let [[this-server this-driver] (new-remote-session {:port 3003}
                                                      {:browser :firefox})]
    (-> this-driver :webdriver (.setLogLevel Level/OFF))
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

(use-fixtures :once start-server start-session-fixture quit-fixture)
(use-fixtures :each reset-browser-fixture)

;; RUN TESTS HERE
(deftest test-common-features-via-remote-server
  (run-common-tests @driver))
