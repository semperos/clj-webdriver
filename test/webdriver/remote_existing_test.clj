(ns ^:manual-setup webdriver.remote-existing-test
  "Tests for RemoteWebDriver server and client (driver) code using manually-started Grid hub"
  (:require [clojure.test :refer :all]
        [clj-webdriver.core :refer [quit to]]
        [clj-webdriver.test.helpers :refer [base-url start-system! stop-system!]]
        [clj-webdriver.test.common :refer [defcommontests]]
        [clj-webdriver.remote.server :refer [init-remote-server new-remote-driver stop]])
  (:import [java.util.logging Level]))

(def drivers (atom nil))

(def server (atom nil))

;; Utilities
(defn hub-host
  []
  (get (System/getenv) "WEBDRIVER_HUB_HOST" "127.0.0.1"))

(defn hub-port
  []
  ;; API default is 4444, so for testing we use 3333
  ;; see scripts/grid-hub and scripts/grid-node
  (Integer/parseInt (get (System/getenv) "WEBDRIVER_HUB_PORT" "3333")))

;; Fixtures
(defn restart-session
  [f]
  (when (not @server)
    (reset! server
            (init-remote-server {:port (hub-port)
                                 :host (hub-host)
                                 :existing true})))
  (when (not @drivers)
    (reset! drivers
            {:firefox (new-remote-driver @server {:browser :firefox})
             :chrome (new-remote-driver @server {:browser :chrome})
             :phantomjs (new-remote-driver @server {:browser :phantomjs})}))
  (doseq [d (vals @drivers)] (to d base-url))
  (f))

(defn quit-session
  [f]
  (f)
  (when @drivers (doseq [d (vals @drivers)] (quit d)))
  (when @server (stop @server)))

(use-fixtures :once start-system! stop-system! quit-session)
(use-fixtures :each restart-session)

(defcommontests "test-firefox-" (:firefox @drivers))
(defcommontests "test-chrome-" (:chrome @drivers))
