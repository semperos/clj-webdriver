(ns ^{:doc "Tests running on SauceLabs using 'Open Sauce' subscription"}
  clj-webdriver.test.saucelabs
  (:require [clojure.test :refer [deftest use-fixtures]]
            [clj-webdriver.core :refer [quit to]]
            [clj-webdriver.test.helpers :refer :all]
            [clj-webdriver.test.common :refer [defcommontests]]
            [clj-webdriver.remote.server :refer [init-remote-server new-remote-driver stop]])
  (:import [java.util.logging Level]
           org.openqa.selenium.Platform
           org.openqa.selenium.remote.DesiredCapabilities))

(def server (atom nil))
(def driver (atom nil))

(defn restart-session
  [f]
  (when (not @server)
    (let [{:keys [host port]} (:saucelabs system)
          this-server (init-remote-server {:host host
                                           :port port
                                           :existing true})
          caps {:version "39"
                :platform Platform/MAC
                :browser-name "firefox"}
          this-driver (new-remote-driver this-server {:browser :firefox
                                                      :capabilities caps})]
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
