(ns clj-webdriver.test.phantomjs
  (:require [clojure.tools.logging :as log]
            [clojure.test :refer :all]
            [clj-webdriver.core :refer [start new-driver to quit]]
            [clj-webdriver.driver :refer [init-driver]]
            [clj-webdriver.test.common :refer [run-common-tests run-phantomjs-tests]]
            [clj-webdriver.test.helpers :refer [base-url start-system! stop-system!]])
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
  (to @phantomjs-driver base-url)
  (f))

(defn quit-browser-fixture
  [f]
  (f)
  (quit @phantomjs-driver))

(use-fixtures :once start-system! stop-system! start-browser-fixture quit-browser-fixture)
(use-fixtures :each reset-browser-fixture)

;; RUN TESTS HERE
(deftest test-common-features-for-phantomjs
  (run-phantomjs-tests @phantomjs-driver))
