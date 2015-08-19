(ns webdriver.phantomjs-test
  (:require [clojure.tools.logging :as log]
            [clojure.test :refer :all]
            [webdriver.core :refer [new-webdriver to quit]]
            [webdriver.test.common :refer [defcommontests]]
            [webdriver.test.helpers :refer [*base-url* start-system! stop-system!]])
  (:import org.openqa.selenium.remote.DesiredCapabilities))

(log/debug "The PhantomJS driver requires a separate download. See https://github.com/detro/ghostdriver for more information if PhantomJS fails to start.")
(def phantomjs-driver (atom nil))

;; Fixtures
(defn restart-browser
  [f]
  (when-not @phantomjs-driver
    (reset! phantomjs-driver
            (new-webdriver {:browser :phantomjs})))
  (to @phantomjs-driver *base-url*)
  (f))

(defn quit-browser
  [f]
  (f)
  (quit @phantomjs-driver))

(use-fixtures :once start-system! stop-system! quit-browser)
(use-fixtures :each restart-browser)

;; RUN TESTS HERE
(defcommontests "test-" @phantomjs-driver)
