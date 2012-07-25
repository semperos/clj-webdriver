(ns clj-webdriver.test.chrome
  (:use clojure.test
        [clj-webdriver.core :only [start new-driver to quit]]
        [clj-webdriver.test.common :only [run-common-tests]]
        [clj-webdriver.test.util :only [start-server]]
        [clj-webdriver.test.config :only [test-base-url]])
  (:require [clojure.tools.logging :as log]))

;; Driver definitions
(log/debug "WARNING: The Chrome driver requires a separate download. See the Selenium-WebDriver wiki for more information if Chrome fails to start.")
(def chrome-driver (new-driver {:browser :chrome}))

;; Fixtures
(defn reset-browser-fixture
  [f]
  (to chrome-driver test-base-url)
  (f))

(defn quit-browser-fixture
  [f]
  (f)
  (quit chrome-driver))

(use-fixtures :once start-server quit-browser-fixture)
(use-fixtures :each reset-browser-fixture)

;; RUN TESTS HERE
(deftest test-common-features-for-chrome
  (doseq [driver [chrome-driver]]
    (run-common-tests driver)))