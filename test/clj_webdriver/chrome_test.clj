(ns clj-webdriver.chrome-test
  (:require [clojure.test :refer :all]
            [clojure.tools.logging :as log]
            [clj-webdriver.test.helpers :refer :all]
            [clj-webdriver.core :refer [start new-driver to quit]]
            [clj-webdriver.driver :refer [init-driver]]
            [clj-webdriver.test.common :refer [run-common-tests]])
  (:import org.openqa.selenium.remote.DesiredCapabilities
           org.openqa.selenium.chrome.ChromeDriver))

;; Driver definitions
(log/debug "WARNING: The Chrome driver requires a separate download. See the Selenium-WebDriver wiki for more information if Chrome fails to start.")
(def chrome-driver (atom nil))

;; Fixtures
(defn restart-browser
  [f]
  (reset! chrome-driver
          (new-driver {:browser :chrome}))
  (to @chrome-driver base-url)
  (f)
  (quit @chrome-driver))

(use-fixtures :once start-system! stop-system!)
(use-fixtures :each restart-browser)

;; RUN TESTS HERE
(deftest test-common-features-for-chrome
  (run-common-tests @chrome-driver))
