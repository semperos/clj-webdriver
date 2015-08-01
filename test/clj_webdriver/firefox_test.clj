(ns clj-webdriver.firefox-test
  (:require [clojure.test :refer :all]
            [clj-webdriver.core :refer [new-driver start current-url find-element find-elements quit get-screenshot with-browser attribute to]]
            [clj-webdriver.driver :refer [driver?]]
            [clj-webdriver.cache :refer [get-cache]]
            [clj-webdriver.test.common :as c]
            [clojure.java.io :as io]
            [clojure.tools.logging :as log]
            [clj-webdriver.cache :as cache]
            [clj-webdriver.firefox :as ff]
            [clj-webdriver.test.helpers :refer [base-url start-system! stop-system!]]))

;; Driver definitions
(def firefox-driver (atom nil))

(def firefox-driver-no-cache (atom nil))

;; Fixtures
(defn restart-browser
  [f]
  (reset! firefox-driver
          (new-driver {:browser :firefox
                       :cache-spec {:strategy :basic,
                                    :args [{}],
                                    :include [{:css "ol#pages"}
                                              {:tag :a, :class "external"}]}}))
  (to @firefox-driver base-url)
  (reset! firefox-driver-no-cache
          (new-driver {:browser :firefox}))
  (to @firefox-driver-no-cache base-url)
  (f)
  (quit @firefox-driver)
  (quit @firefox-driver-no-cache))

(defn seed-driver-cache
  [f]
  (cache/seed @firefox-driver {:url (current-url @firefox-driver)})
  (f))

(use-fixtures :once start-system! stop-system!)
(use-fixtures :each restart-browser seed-driver-cache)

(c/defcommontests "test-cache-" @firefox-driver)
(c/defcommontests "test-" @firefox-driver-no-cache)

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
                       base-url)]
    (is (not (cache/cacheable? temp-dr {:css "ol#pages"})))
    (find-elements temp-dr {:css "ol#pages"})
    (is (empty? (dissoc @(get-in temp-dr [:cache-spec :cache]) :url)))
    (quit temp-dr)))

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
