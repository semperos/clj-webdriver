(ns clj-webdriver.test.firefox
  (:use clojure.test
        [clj-webdriver.core :only [new-driver start current-url find-element find-elements quit get-screenshot with-browser attribute to]]
        [clj-webdriver.driver :only [driver?]]
        [clj-webdriver.cache :only [get-cache]]
        [clj-webdriver.test.common :only [run-common-tests]]
        [clj-webdriver.test.util :only [start-server]]
        [clj-webdriver.test.config :only [base-url]])
  (:require [clj-webdriver.cache :as cache]
            [clj-webdriver.firefox :as ff]
            [clojure.java.io :as io]
            [clojure.tools.logging :as log]))

;; Driver definitions
(def firefox-driver (atom nil))

(def firefox-driver-no-cache (atom nil))

;; Fixtures
(defn start-browser-fixture
  [f]
  (reset! firefox-driver
          (new-driver {:browser :firefox
                       :cache-spec {:strategy :basic,
                                    :args [{}],
                                    :include [{:css "ol#pages"}
                                              {:tag :a, :class "external"}]}}))
  (reset! firefox-driver-no-cache
          (new-driver {:browser :firefox}))
  (f))

(defn reset-browser-fixture
  [f]
  (to @firefox-driver (base-url))
  (to @firefox-driver-no-cache (base-url))
  (f))

(defn quit-browser-fixture
  [f]
  (f)
  (quit @firefox-driver)
  (quit @firefox-driver-no-cache))

(defn seed-driver-cache-fixture
  [f]
  (cache/seed @firefox-driver {:url (current-url @firefox-driver)})
  (f))

(use-fixtures :once start-server start-browser-fixture quit-browser-fixture)
(use-fixtures :each reset-browser-fixture seed-driver-cache-fixture)

;; RUN TESTS HERE
(deftest test-common-features-for-firefox
  (doseq [driver [@firefox-driver
                  @firefox-driver-no-cache]]
    (run-common-tests driver)))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;                            ;;;
;;; SPECIAL CASE FUNCTIONALITY ;;;
;;;                            ;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; Includes cache-support and browser-specific settings

;; Caching Functionality
(deftest test-cache-initialization
  (is (cache/cache-enabled? @firefox-driver)))

(deftest test-cache-insert-retrieve-delete
  (cache/insert @firefox-driver {:query [:alpha]} "beta")
  (is (cache/in-cache? @firefox-driver {:query [:alpha]}))
  (is (= (cache/retrieve @firefox-driver :alpha) '("beta")))
  (cache/delete @firefox-driver :alpha)
  (is (nil? (cache/retrieve @firefox-driver :alpha))))

(deftest test-cache-seed
  (cache/seed @firefox-driver {{:query [:foo]} "clojure"})
  (is (= (cache/retrieve @firefox-driver :foo) "clojure"))
  (cache/seed @firefox-driver)
  (is (= @(get-cache @firefox-driver) {:url (current-url @firefox-driver)})))

(deftest test-cacheable?
  (is (cache/cacheable? @firefox-driver {:tag :a, :class "external"}))
  (is (cache/cacheable? @firefox-driver {:css "ol#pages"}))
  (is (not (cache/cacheable? @firefox-driver :table)))
  (is (not (cache/cacheable? @firefox-driver {:css "#pages"})))
  (find-elements @firefox-driver {:tag :a, :class "external"})
  (is (not-empty (dissoc @(get-in @firefox-driver [:cache-spec :cache]) :url)))
  (cache/delete @firefox-driver {:tag :a, :class "external"})
  (is (empty? (dissoc @(get-in @firefox-driver [:cache-spec :cache]) :url)))
  (find-elements @firefox-driver {:css "ol#pages"})
  (is (not-empty (dissoc @(get-in @firefox-driver [:cache-spec :cache]) :url))))

(deftest test-cache-excludes
  ;; includes are tested by default
  (let [temp-dr (start {:browser :firefox
                        :cache-spec {:strategy :basic,
                                     :args [{}],
                                     :exclude [ {:css "ol#pages"} ]}}
                       (base-url))]
    (is (not (cache/cacheable? temp-dr {:css "ol#pages"})))
    (find-elements temp-dr {:css "ol#pages"})
    (is (empty? (dissoc @(get-in temp-dr [:cache-spec :cache]) :url)))
    (quit temp-dr)))

;; Firefox-specific Functionality

(deftest firefox-should-support-custom-profiles
  (is (with-browser [tmp-dr (start {:browser :firefox
                                    :profile (ff/new-profile)}
                                   (base-url))]
        (log/info "[x] Starting Firefox with custom profile.")
        (driver? tmp-dr))))

;; (deftest firefox-should-support-extensions
;;   (is (with-browser [tmp-dr (start {:browser :firefox
;;                                     :profile (doto (ff/new-profile)
;;                                                (ff/enable-extension :firebug))}
;;                                    (base-url))]
;;         (log/info "[x] Starting Firefox with extensions.")
;;         (driver? tmp-dr))))
