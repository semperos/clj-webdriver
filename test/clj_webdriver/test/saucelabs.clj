(ns ^{:doc "Tests running on SauceLabs using 'Open Sauce' subscription"}
  clj-webdriver.test.saucelabs
  (:require [clojure.test :refer [deftest use-fixtures]]
            [clj-webdriver.core :refer [quit to]]
            [clj-webdriver.properties :refer [*properties*]]
            [clj-webdriver.test.config :refer [base-url]]
            [clj-webdriver.test.common :refer [run-common-tests]]
            [clj-webdriver.remote.server :refer [new-remote-session stop]])
  (:import [java.util.logging Level]))

(def server (atom nil))
(def driver (atom nil))

(defn start-session-fixture
  [f]
  (let [{:keys [host port]} (:saucelabs *properties*)
        [this-server this-driver] (new-remote-session {:host host
                                                       :port port
                                                       :existing true}
                                                      {:browser :firefox})]
    (-> this-driver :webdriver (.setLogLevel Level/OFF))
    (reset! server this-server)
    (reset! driver this-driver))
  (f))

(defn reset-browser-fixture
  [f]
  (to @driver (base-url))
  (f))

(defn quit-fixture
  [f]
  (f)
  (quit @driver)
  (stop @server))

(use-fixtures :once start-session-fixture quit-fixture)
(use-fixtures :each reset-browser-fixture)

;; RUN TESTS HERE
(deftest test-common-features-via-remote-server
  (run-common-tests @driver))
