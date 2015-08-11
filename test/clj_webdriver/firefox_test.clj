(ns clj-webdriver.firefox-test
  (:require [clojure.test :refer :all]
            [clj-webdriver.core :refer [new-driver start current-url find-element find-elements quit get-screenshot with-browser attribute to]]
            [clj-webdriver.driver :refer [driver?]]
            [clj-webdriver.test.common :as c]
            [clojure.java.io :as io]
            [clojure.tools.logging :as log]
            [clj-webdriver.firefox :as ff]
            [clj-webdriver.test.helpers :refer [base-url start-system! stop-system!]]))

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
  (is (with-browser [tmp-dr (start {:browser :firefox
                                    :profile (ff/new-profile)}
                                   base-url)]
        (log/info "[x] Starting Firefox with custom profile.")
        (driver? tmp-dr))))

;; (deftest firefox-should-support-extensions
;;   (is (with-browser [tmp-dr (start {:browser :firefox
;;                                     :profile (doto (ff/new-profile)
;;                                                (ff/enable-extension :firebug))}
;;                                    base-url)]
;;         (log/info "[x] Starting Firefox with extensions.")
;;         (driver? tmp-dr))))
