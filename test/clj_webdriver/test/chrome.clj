(ns clj-webdriver.test.chrome
  (:use clojure.test
        [clj-webdriver.core :only [start new-driver to quit]]
        [clj-webdriver.driver :only [init-driver]]
        [clj-webdriver.test.common :only [run-common-tests]]
        [clj-webdriver.test.util :only [chromium-preferred? start-server]]
        [clj-webdriver.test.config :only [base-url]])
  (:require [clojure.tools.logging :as log])
  (:import org.openqa.selenium.remote.DesiredCapabilities
           org.openqa.selenium.chrome.ChromeDriver))

;; Driver definitions
(log/debug "WARNING: The Chrome driver requires a separate download. See the Selenium-WebDriver wiki for more information if Chrome fails to start.")
(def chrome-driver (atom nil))

;; Fixtures
(defn start-browser-fixture
  [f]
  (if (chromium-preferred?)
    (reset! chrome-driver
            (init-driver
             (ChromeDriver. (doto (DesiredCapabilities/chrome)
                              (.setCapability "chrome.binary"
                                              "/usr/lib/chromium-browser/chromium-browser")))))
    (reset! chrome-driver
          (new-driver {:browser :chrome})))
  (f))

(defn reset-browser-fixture
  [f]
  (to @chrome-driver (base-url))
  (f))

(defn quit-browser-fixture
  [f]
  (f)
  (quit @chrome-driver))

(use-fixtures :once start-server start-browser-fixture quit-browser-fixture)
(use-fixtures :each reset-browser-fixture)

;; RUN TESTS HERE
(deftest test-common-features-for-chrome
  (run-common-tests @chrome-driver))