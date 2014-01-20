(ns clj-webdriver.test.phantomjs
  (:use clojure.test
        [clj-webdriver.core :only [start new-driver to quit]]
        [clj-webdriver.driver :only [init-driver]]
        [clj-webdriver.test.common :only [run-common-tests run-phantomjs-tests]]
        [clj-webdriver.test.util :only [start-server]]
        [clj-webdriver.test.config :only [base-url]])
  (:require [clojure.tools.logging :as log])
  (:import org.openqa.selenium.remote.DesiredCapabilities))

;; Driver definitions
(log/debug "WARNING: The PhantomJS driver requires a separate download. See https://github.com/detro/ghostdriver for more information if PhantomJS fails to start.")
(def phantomjs-driver (atom nil))

;; Fixtures
(defn start-browser-fixture
  [f]
  (reset! phantomjs-driver
          (new-driver {:browser :phantomjs}))
  (f))

(defn reset-browser-fixture
  [f]
  (to @phantomjs-driver (base-url))
  (f))

(defn quit-browser-fixture
  [f]
  (f)
  (quit @phantomjs-driver))

(use-fixtures :once start-server start-browser-fixture quit-browser-fixture)
(use-fixtures :each reset-browser-fixture)

;; RUN TESTS HERE
(deftest test-common-features-for-phantomjs
  (run-phantomjs-tests @phantomjs-driver))
