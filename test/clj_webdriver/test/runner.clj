(ns ^{:doc "Custom Clojure test runner; writes results to log"}
  clj-webdriver.test.runner
  (:use clojure.test)
  (:require [clj-time.local :as t]
            [clojure.java.io :as io]
            [clojure.pprint :as pp]
            clj-webdriver.test.chrome
            clj-webdriver.test.firefox
            clj-webdriver.test.phantomjs
            clj-webdriver.test.remote
            clj-webdriver.test.remote-existing
            clj-webdriver.test.taxi :reload
            clj-webdriver.test.wire))

(def log-file "test.log")

(defn timestamp
  "Return string timestamp for printing to log file for each test run."
  []
  (str (t/local-now)))

(defn format-test-preface
  [name]
  (let [test-sep "################################################################################\n"]
    (str "\n"
         test-sep
         "## TEST RUN FOR " name "\n"
         "## " (timestamp) "\n"
         test-sep)))

(defn format-subtitle
  [name]
  (str "###\n"
       "# " name "\n"
       "###\n"))

(defn run-custom
  "Given a human-readable name and the namespace to test, format stdout and final results as required."
  [ns]
  (let [{:keys [test pass fail error]} (run-tests ns)]
    (format "Total Tests: %d\nPass: %d\nFail: %d\nError: %d\n\n"
            test pass fail error)))

(defn run-template
  "Master fn for authoring new custom runners. Given a component name followed by a vector of maps with a descriptive title and test namespace pairings, run the tests and print output to a log file."
  [name title-ns-pairs]
  (with-open [w (io/writer log-file :append true)]
    (.write w (format-test-preface name))
    (binding [*out* w
              *err* w]
      (doseq [{:keys [title ns]} title-ns-pairs]
        (.write w (str (format-subtitle title)
                       (run-custom ns)))))))

(defn run-core
  []
  (run-template "core" [{:title "Firefox Results"
                         :ns 'clj-webdriver.test.firefox}
                        {:title "Chrome Results"
                         :ns 'clj-webdriver.test.chrome}]))

(defn run-phantomjs
  []
  (run-template "phantomjs" [{:title "PhantomJS Results"
                              :ns 'clj-webdriver.test.phantomjs}]))

(defn run-remote
  []
  (run-template "remote" [{:title "Remote for Existing Grid Results"
                           :ns 'clj-webdriver.test.remote-existing}
                          {:title "Remote for Managed Grid Results"
                           :ns 'clj-webdriver.test.remote}
                          {:title "Wire API Results"
                           :ns 'clj-webdriver.test.wire}]))

(defn run-taxi
  []
  (run-template "taxi" [{:title "Taxi Results"
                         :ns 'clj-webdriver.test.taxi}]))

(defn run-all
  []
  (run-core)
  (run-remote)
  (run-taxi))
