(ns ^{:doc "Custom Clojure test runner; writes results to log"}
  clj-webdriver.test.runner
  (:use clojure.test)
  (:require [clj-time.local :as t]
            [clojure.java.io :as io]
            [clojure.pprint :as pp]
            clj-webdriver.test.chrome
            clj-webdriver.test.firefox
            clj-webdriver.test.remote
            clj-webdriver.test.remote-existing
            clj-webdriver.test.taxi
            clj-webdriver.test.wire))

(def log-file "test.log")

(defn timestamp
  "Return string timestamp for printing to log file for each test run."
  []
  (str (t/local-now)))

(defn format-test-preface
  [name]
  (let [test-sep "################################################################################\n"]
    (str test-sep
         (timestamp)
         "TEST RUN FOR "
         name
         test-sep)))

(defn run-custom
  "Given a human-readable name and the namespace to test, format stdout and final results as required."
  [name ns]
  (str name " Results: "
       (with-out-str
         (pp/pprint (run-tests ns)))))

(defn run-core
  []
  (with-open [w (io/writer log-file :append true)]
    (.write w (format-test-preface "core"))
    (binding [*out* w
              *err* w]
      (.write w (run-custom "Firefox" 'clj-webdriver.test.firefox))
      (.write w (run-custom "Chrome" 'clj-webdriver.test.chrome)))))

(defn run-core [])
(defn run-remote [])
(defn run-taxi [])

(defn run-all
  []
  (run-core)
  (run-remote)
  (run-taxi))