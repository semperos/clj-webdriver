(ns webdriver.firefox-test
  (:require [clojure.test :refer :all]
            [webdriver.core :refer [new-driver current-url find-element find-elements quit get-screenshot attribute to with-driver]]
            webdriver.driver
            [webdriver.test.common :as c]
            [clojure.java.io :as io]
            [clojure.tools.logging :as log]
            [webdriver.firefox :as ff]
            [webdriver.test.helpers :refer [base-url start-system! stop-system!]])
  (:import webdriver.driver.Driver))

;; Driver definitions
(def firefox-driver (atom nil))

;; Fixtures
(defn restart-browser
  [f]
  (when-not @firefox-driver
    (reset! firefox-driver
            (new-driver {:browser :firefox})))
  (to @firefox-driver base-url)
  (f))

(defn quit-browser
  [f]
  (f)
  (quit @firefox-driver))

(use-fixtures :once start-system! stop-system! quit-browser)
(use-fixtures :each restart-browser)

(c/defcommontests "test-" @firefox-driver)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;                            ;;;
;;; SPECIAL CASE FUNCTIONALITY ;;;
;;;                            ;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; Firefox-specific Functionality

(deftest firefox-should-support-custom-profiles
  (is (with-driver [tmp-dr (new-driver {:browser :firefox
                                        :profile (ff/new-profile)})]
        (log/info "[x] Starting Firefox with custom profile.")
        (instance? Driver tmp-dr))))
