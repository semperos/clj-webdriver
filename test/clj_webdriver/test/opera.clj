(ns clj-webdriver.test.opera
  (:use clojure.test
        [clj-webdriver.core :only [start to quit]]
        [clj-webdriver.test.common :only [run-common-tests]]
        [clj-webdriver.test.util :only [start-server]]
        [clj-webdriver.test.config :only [test-base-url]]))

;; Driver definitions
(def opera-driver (start {:browser :opera} test-base-url))

;; Fixtures
(defn reset-browser-fixture
  [f]
  (to opera-driver test-base-url)
  (f))

(defn quit-browser-fixture
  [f]
  (f)
  (quit opera-driver))

(use-fixtures :once start-server quit-browser-fixture)
(use-fixtures :each reset-browser-fixture)

;; RUN TESTS HERE
(deftest test-common-features-for-opera
  (doseq [driver [opera-driver]]
    (run-common-tests driver)))