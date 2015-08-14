(ns ^:chrome clj-webdriver.chrome-test
  (:require [clojure.test :refer :all]
            [clojure.tools.logging :as log]
            [clj-webdriver.test.helpers :refer :all]
            [clj-webdriver.core :refer [start new-driver to quit]]
            [clj-webdriver.test.common :as c])
  (:import org.openqa.selenium.remote.DesiredCapabilities
           org.openqa.selenium.chrome.ChromeDriver))

;; Driver definitions
(log/debug "The Chrome driver requires a separate download. See the Selenium-WebDriver wiki for more information if Chrome fails to start.")
(def chrome-driver (atom nil))

;; Fixtures
(defn restart-browser
  [f]
  (when-not @chrome-driver
    (reset! chrome-driver
            (new-driver {:browser :chrome})))
  (to @chrome-driver base-url)
  (f))

(defn quit-browser
  [f]
  (f)
  (quit @chrome-driver))

(use-fixtures :once start-system! stop-system! quit-browser)
(use-fixtures :each restart-browser)

(c/defcommontests "test-" @chrome-driver)
