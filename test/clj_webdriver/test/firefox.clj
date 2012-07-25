(ns clj-webdriver.test.firefox
  (:use clojure.test
        [clj-webdriver.core :only [new-driver start current-url find-element quit get-screenshot with-browser attribute to]]
        [clj-webdriver.driver :only [get-cache driver?]]
        [clj-webdriver.test.common :only [run-common-tests]]
        [clj-webdriver.test.util :only [start-server]]
        [clj-webdriver.test.config :only [test-base-url]])
  (:require [clj-webdriver.cache :as cache]
            [clj-webdriver.firefox :as ff]
            [clojure.java.io :as io]
            [clojure.tools.logging :as log]))

;; Driver definitions
(def firefox-driver (new-driver {:browser :firefox
                                 :cache-spec {:strategy :basic,
                                              :args [{}],
                                              :include [ (fn [element] (= (attribute element :class) "external"))
                                                         {:css "ol#pages"}]}}))

(def firefox-driver-no-cache (new-driver {:browser :firefox}))

;; Fixtures
(defn reset-browser-fixture
  [f]
  (to firefox-driver test-base-url)
  (to firefox-driver-no-cache test-base-url)
  (f))

(defn quit-browser-fixture
  [f]
  (f)
  (quit firefox-driver)
  (quit firefox-driver-no-cache))

(defn seed-driver-cache-fixture
  [f]
  (cache/seed firefox-driver {:url (current-url firefox-driver), {:query [:foo]} "bar"})
  (f))

(use-fixtures :once start-server quit-browser-fixture)
(use-fixtures :each reset-browser-fixture seed-driver-cache-fixture)

;; RUN TESTS HERE
(deftest test-common-features-for-firefox
  (doseq [driver [firefox-driver
                  firefox-driver-no-cache]]
    (run-common-tests driver)))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;                            ;;;
;;; SPECIAL CASE FUNCTIONALITY ;;;
;;;                            ;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; Includes cache-support and browser-specific settings

;; Caching Functionality
(deftest test-cache-initialization
  (is (cache/cache-enabled? firefox-driver)))

(deftest test-cache-insert
  ;; insert was used to seed the data in the test fixture; test now for presence
  (is (= (get @(get-cache firefox-driver) {:query [:foo]}) "bar"))
  (is (nil? (get @(get-cache firefox-driver) :wowza))))

(deftest test-in-cache?
  (is (cache/in-cache? firefox-driver {:query [:foo]}))
  (is (not (cache/in-cache? firefox-driver :wowza))))

(deftest test-cache-retrieve
  (is (= (cache/retrieve firefox-driver :foo) "bar"))
  (is (nil? (cache/retrieve firefox-driver :wowza))))

(deftest test-cache-delete
  (cache/insert firefox-driver {:query [:alpha]} "beta")
  (is (= (cache/retrieve firefox-driver :alpha) "beta"))
  (cache/delete firefox-driver :alpha)
  (is (nil? (cache/retrieve firefox-driver :alpha))))

(deftest test-cache-seed
  (cache/seed firefox-driver {{:query [:foo]} "clojure"})
  (is (= (cache/retrieve firefox-driver :foo) "clojure"))
  (cache/seed firefox-driver)
  (is (= @(get-cache firefox-driver) {:url (current-url firefox-driver)})))

(deftest test-cacheable?
  (is (cache/cacheable? firefox-driver (find-element firefox-driver {:tag :a, :class "external"})))
  (is (not (cache/cacheable? firefox-driver {:class "external"})))
  (is (cache/cacheable? firefox-driver {:css "ol#pages"}))
  (is (not (cache/cacheable? firefox-driver :table)))
  (is (not (cache/cacheable? firefox-driver {:css "#pages"}))))

(deftest test-cache-excludes
  ;; includes are tested by default
  (let [temp-dr (start {:browser :firefox
                        :cache-spec {:strategy :basic,
                                     :args [{}],
                                     :exclude [ (fn [element] (= (attribute element :class) "external")),
                                                {:css "ol#pages"}]}}
                       test-base-url)]
    (is (cache/cacheable? temp-dr (find-element temp-dr {:tag :table})))
    (is (cache/cacheable? temp-dr (find-element temp-dr {:css "#pages"})))
    (is (not (cache/cacheable? temp-dr (find-element temp-dr {:tag :a, :class "external"}))))
    (is (not (cache/cacheable? temp-dr {:css "ol#pages"})))
    (quit temp-dr)))


;; TODO: write tests for screenshot functionality, ensure diff outputs
(deftest test-screenshot-should-return-different-outputs
  (is (string? (get-screenshot firefox-driver :base64)))
  (is (> (count (get-screenshot firefox-driver :bytes)) 0))
  (is (= (class (get-screenshot firefox-driver :file)) java.io.File))
  (is (= (class (get-screenshot firefox-driver :file "/tmp/screenshot_test.png")) java.io.File))
  ;; the following will throw an exception if deletion fails, hence our test
  (io/delete-file "/tmp/screenshot_test.png"))

;; Firefox-specific Functionality

(deftest firefox-should-support-custom-profiles
  (is (with-browser [tmp-dr (start {:browser :firefox
                                    :profile (ff/new-profile)}
                                   test-base-url)]
        (log/info "[x] Starting Firefox with custom profile.")
        (driver? tmp-dr))))

(deftest firefox-should-support-extensions
  (is (with-browser [tmp-dr (start {:browser :firefox
                                    :profile (doto (ff/new-profile)
                                               (ff/enable-extension :firebug))}
                                   test-base-url)]
        (log/info "[x] Starting Firefox with extensions.")
        (driver? tmp-dr))))