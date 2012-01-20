(ns clj-webdriver.test.core
  (:use [clj-webdriver core util window-handle wait options form-helpers]
        [clj-webdriver.driver :only [get-cache driver?]])
  (:use [ring.adapter.jetty :only [run-jetty]]
        clojure.test)
  (:require [clj-webdriver.test.example-app.core :as web-app]
            [clj-webdriver.cache :as cache]
            [clj-webdriver.firefox :as ff]
            [clojure.tools.logging :as log]
            [clojure.java.io :as io])
  (:import [clj_webdriver.driver.Driver]
           [org.openqa.selenium TimeoutException]))

;; ## Setup ##
(def test-port 5744)
(def test-host "localhost")
(def test-base-url (str "http://" test-host ":" test-port "/"))

(def firefox-driver (to (new-driver {:browser :firefox
                         :cache-spec {:strategy :basic,
                                      :args [],
                                      :include [ (fn [element] (= (attribute element :class) "external"))
                                                 {:css "ol#pages"}]}})
            test-base-url))

(def firefox-driver-no-cache (to (new-driver {:browser :firefox}) test-base-url))
(log/debug "WARNING: The Chrome driver requires a separate download. See the Selenium-WebDriver wiki for more information if Chrome fails to start.")
(def chrome-driver (start {:browser :chrome} test-base-url))
(def opera-driver (start {:browser :opera} test-base-url))

;; clojure.test fixtures
(defn start-server [f]
  (loop [server (run-jetty #'web-app/routes {:port test-port, :join? false})]
    (if (.isStarted server)
      (do
        (f)
        (.stop server))
      (recur server))))

(defn reset-browser-fixture
  [f]
  (to firefox-driver test-base-url)
  (to firefox-driver-no-cache test-base-url)
  (to chrome-driver test-base-url)
  (to opera-driver test-base-url)
  (f))

(defn quit-browser-fixture
  [f]
  (f)
  (quit firefox-driver)
  (quit firefox-driver-no-cache)
  (quit chrome-driver)
  (quit opera-driver))

(defn seed-driver-cache-fixture
  [f]
  (cache/seed firefox-driver {:url (current-url firefox-driver), {:query [:foo]} "bar"})
  (f))

(use-fixtures :once start-server quit-browser-fixture)
(use-fixtures :each reset-browser-fixture seed-driver-cache-fixture)

;; Utilities
(defmacro thrown?
  "Return truthy if the exception in `klass` is thrown, otherwise return falsey (nil) (code adapted from clojure.test)"
  [klass & forms]
  `(try ~@forms
        false
        (catch ~klass e#
          true)))

;;;;;;;;;;;;;;;;;;;;;;;;
;;;                  ;;;
;;; Test Definitions ;;;
;;;                  ;;;
;;;;;;;;;;;;;;;;;;;;;;;;
(defn test-browser-basics
  [driver]
  (is (= clj_webdriver.driver.Driver (class driver)))
  (is (= test-base-url (current-url driver)))
  (is (= "Ministache" (title driver)))
  (is (boolean (re-find #"(?i)html>" (page-source driver)))))

(defn back-forward-should-traverse-browser-history
  [driver]
  (-> driver
      (find-it {:tag :a, :text "example form"})
      click)
  (is (= (str test-base-url "example-form") (current-url driver)))
  (back driver)
  (is (= test-base-url (current-url driver)))
  (forward driver)
  (is (= (str test-base-url "example-form") (current-url driver))))

(defn to-should-open-given-url-in-browser
  [driver]
  (to driver (str test-base-url "example-form"))
  (is (= (str test-base-url "example-form") (current-url driver)))
  (is (= "Ministache" (title driver))))

(defn should-be-able-to-find-elements-using-low-level-by-wrappers
  [driver]
  (-> driver
      (find-it {:tag :a, :text "example form"})
      click)
  (is (= "first_name"
         (attribute (find-element driver (by-id "first_name")) :id)))
  (is (= "home"
         (text (find-element driver (by-link-text "home")))))
  (is (= "example form"
         (text (find-element driver (by-partial-link-text "example")))))
  (is (= "first_name"
         (attribute (find-element driver (by-name "first_name")) :id)))
  (is (= "home"
         (text (find-element driver (by-tag "a")))))
  (is (= "home"
         (text (find-element driver (by-xpath "//a[text()='home']")))))
  (is (= "home"
         (text (find-element driver (by-class-name "menu-item")))))
  (is (= "home"
         (text (find-element driver (by-css-selector "#footer a.menu-item")))))
  (to driver test-base-url)
  (is (= "first odd"
         (attribute (find-element driver (by-class-name "first odd")) :class))))

(defn find-it-should-support-basic-attr-val-map
  [driver]
  (is (= "Moustache"
         (text (nth (find-them driver {:tag :a}) 1))))
  (is (= "Moustache"
         (text (find-it driver {:class "external"}))))
  (is (= "first odd"
         (attribute (find-it driver {:class "first odd"}) :class)))
  (is (= "first odd"
         (attribute (find-it driver {:tag :li, :class "first odd"}) :class)))
  (is (= "https://github.com/cgrand/moustache"
         (attribute (find-it driver {:text "Moustache"}) "href")))
  (is (= 8
         (count (find-them driver {:tag :a}))))
  (-> driver
      (find-it {:tag :a, :text "example form"})
      click)
  (is (= "first_name"
         (attribute (find-it driver {:type "text"}) "id")))
  (is (= "first_name"
         (attribute (find-it driver {:tag :input, :type "text"}) "id")))
  (is (= "first_name"
         (attribute (find-it driver {:tag :input, :type "text", :name "first_name"}) "id")))
  (is (= "first_name"
         (attribute (find-it driver {:tag :input, :type "text", :name #"first_"}) "id")))
  (is (= "last_name"
         (attribute (find-it driver {:tag :input, :type "text", :name #"last_"}) "id")))
  (is (= "Smith"
         (attribute (find-it driver {:tag :input, :type "text", :name #"last_"}) "value")))
  (is (= "Smith"
         (attribute (find-it driver {:tag :input, :type "text", :name #"last_"}) "value"))))

(defn find-it-should-support-regexes-in-attr-val-map
  [driver]
  (is (= "Moustache"
         (text (find-it driver {:tag :a, :class #"exter"}))))
  (is (= "Moustache"
         (text (find-it driver {:tag :a, :text #"Mous"}))))
  (is (= "Moustache"
         (text (find-it driver {:tag :a, :class "external", :href #"github"}))))
  (is (= "Moustache"
         (text (find-it driver {:tag :a, :class #"exter", :href #"github"}))))
  (is (= 3
         (count (find-them driver {:class #"-item"}))))
  (is (= 3
         (count (find-them driver {:tag :a, :class #"-item"}))))
  (is (= 1
         (count (find-them driver {:tag :a, :text #"hom"}))))
  (is (= 1
         (count (find-them driver {:tag :a, :text #"(?i)HOM"}))))
  (is (= 2
         (count (find-them driver {:tag :a, :class #"exter", :href #"github"})))))


(defn find-it-should-support-hierarchical-querying
  [driver]
  (is (= "Moustache"
         (text (find-it driver [{:tag :div, :id "content"}, {:tag :a, :class "external"}]))))
  (is (= "Moustache"
         (text (find-it driver [{:tag :div, :id "content"}, {:tag :a, :class #"exter"}]))))
  (is (= "Moustache"
         (text (find-it driver [{:tag :div, :id "content"}, {:tag :a, :href #"github"}]))))
  (is (= "home"
         (text (find-it driver [{:tag :*, :id "footer"}, {:tag :a}]))))
  (is (= 3
         (count (find-them driver [{:tag :*, :id "footer"}, {:tag :a}]))))
  (is (= 2
         (count (find-them driver [{:tag :div, :id "content"}, {:tag :a, :class #"exter"}])))))

(defn exists-should-return-truthy-falsey-and-should-not-throw-an-exception
  [driver]
  (is (-> driver
          (find-it {:tag :a})
          exists?))
  (is (not
       (-> driver
           (find-it {:tag :area})
           exists?))))

(defn visible-should-return-truthy-falsey-when-visible
  [driver]
  (is (-> driver
          (find-it {:tag :a, :text "Moustache"})
          visible?))
  (is (not
       (-> driver
           (find-it {:tag :a, :href "#pages"})
           visible?)))
  (is (-> driver
          (find-it {:tag :a, :text "Moustache"})
          displayed?))
  (is (not
       (-> driver
           (find-it {:tag :a, :href "#pages"})
           displayed?))))

(defn present-should-return-truthy-falsey-when-exists-and-visible
  [driver]
  (is (-> driver
          (find-it {:tag :a, :text "Moustache"})
          present?))
  (is (not
       (-> driver
           (find-it {:tag :a, :href "#pages"})
           present?))))

;; Default wrap for strings is double quotes
(defn generated-xpath-should-wrap-strings-in-double-quotes
  [driver]
  (is (find-it driver {:text "File's Name"})))

(defn xpath-function-should-return-string-xpath-of-element
  [driver]
  (is (= (xpath (find-it driver {:tag :a, :text "Moustache"})) "/html/body/div[2]/div/p/a")))

(defn html-function-should-return-string-html-of-element
  [driver]
  (is (re-find #"href=\"https://github\.com/cgrand/moustache\"" (html (find-it driver {:tag :a, :text "Moustache"})))))

(defn find-table-cell-should-find-cell-with-coords
  [driver]
  (is (= "th"
         (.toLowerCase (tag (find-table-cell driver {:id "pages-table"} [0 0])))))
  (is (= "th"
         (.toLowerCase (tag (find-table-cell driver {:id "pages-table"} [0 1])))))
  (is (= "td"
         (.toLowerCase (tag (find-table-cell driver {:id "pages-table"} [1 0])))))
  (is (= "td"
         (.toLowerCase (tag (find-table-cell driver {:id "pages-table"} [1 1]))))))

(defn find-table-row-should-find-all-cells-for-row
  [driver]
  (is (= 2
         (count (find-table-row driver {:id "pages-table"} 0))))
  (is (= "th"
         (.toLowerCase (tag (first (find-table-row driver {:id "pages-table"} 0))))))
  (is (= "td"
         (.toLowerCase (tag (first (find-table-row driver {:id "pages-table"} 1)))))))

(defn test-form-elements
  [driver]
  (to driver (str test-base-url "example-form"))
  ;; Clear element
  (-> driver
      (find-it [{:tag :form, :id "example_form"}, {:tag :input, :name #"last_"}])
      clear)
  (is (= ""
         (value (find-it driver [{:tag :form, :id "example_form"}, {:tag :input, :name #"last_"}]))))
  ;; Radio buttons
  (is (= true
         (selected? (find-it driver {:tag :input, :type "radio", :value "male"}))))
  (-> driver
      (find-it {:tag :input, :type "radio", :value "female"})
      select)
  (is (= true
         (selected? (find-it driver {:tag :input, :type "radio", :value "female"}))))
  (-> driver
      (find-it {:tag :radio, :value "male"})
      select)
  (is (= true
         (selected? (find-it driver {:tag :input, :type "radio", :value "male"}))))
  ;; Checkboxes
  (is (= false
         (selected? (find-it driver {:tag :input, :type "checkbox", :name #"(?i)clojure"}))))
  (-> driver
      (find-it {:tag :input, :type "checkbox", :name #"(?i)clojure"})
      toggle)
  (is (= true
         (selected? (find-it driver {:tag :input, :type "checkbox", :name #"(?i)clojure"}))))
  (-> driver
      (find-it {:tag :checkbox, :name #"(?i)clojure"})
      click)
  (is (= false
         (selected? (find-it driver {:tag :input, :type "checkbox", :name #"(?i)clojure"}))))
  (-> driver
      (find-it {:tag :checkbox, :type "checkbox", :name #"(?i)clojure"})
      select)
  (is (= true
         (selected? (find-it driver {:tag :input, :type "checkbox", :name #"(?i)clojure"}))))
  ;; Text fields
  (-> driver
      (find-it {:tag :input, :id "first_name"})
      (input-text "foobar"))
  (is (= "foobar"
         (value (find-it driver {:tag :input, :id "first_name"}))))
  (-> driver
      (find-it {:tag :textfield, :id "first_name"})
      clear
      (input-text "clojurian"))
  (is (= "clojurian"
         (value (find-it driver {:tag :textfield, :id "first_name"}))))
  ;; Boolean attributes (disabled, readonly, etc)
  (is (= "disabled"
         (attribute (find-it driver {:id "disabled_field"}) :disabled)))
  (is (= "readonly"
         (attribute (find-it driver {:id "purpose_here"}) :readonly)))
  (is (nil?
       (attribute (find-it driver {:id "disabled_field"}) :readonly)))
  (is (nil?
       (attribute (find-it driver {:id "purpose_here"}) :disabled))))

(defn quick-fill-should-accept-special-seq-and-perform-batch-actions-on-form
  [driver]
  (to driver (str test-base-url "example-form"))
  (quick-fill driver
              [{"first_name" clear}
               {"first_name" "Richard"}
               {{:id "last_name"} clear}
               {{:id "last_name"} "Hickey"}
               {{:name "bio"} clear}
               {{:name "bio"} #(input-text % "Creator of Clojure")}
               {{:tag "input", :type "radio", :value "female"} click}
               {{:css "select#countries"} #(select-by-value % "france")}])
  (is (= "Richard"
         (value (find-it driver {:tag :input, :id "first_name"}))))
  (is (= "Hickey"
         (value (find-it driver {:tag :input, :id "last_name"}))))
  (is (= "Creator of Clojure"
         (value (find-it driver {:tag :textarea, :name "bio"}))))
  (is (selected?
       (find-it driver {:tag :input, :type "radio", :value "female"})))
  (is (selected?
       (find-it driver {:tag :option, :value "france"}))))

(defn quick-fill-submit-should-always-return-nil
  [driver]
  (to driver (str test-base-url "example-form"))
  (is (nil?
       (quick-fill-submit driver
                          [{"first_name" clear}
                           {"first_name" "Richard"}
                           {{:id "last_name"} clear}
                           {{:id "last_name"} "Hickey"}
                           {{:name "bio"} clear}
                           {{:name "bio"} #(input-text % "Creator of Clojure")}
                           {{:tag "input", :type "radio", :value "female"} click}
                           {{:css "select#countries"} #(select-by-value % "france")}]))))

(defn should-be-able-to-toggle-between-open-windows
  [driver]
  (is (= 1
         (count (window-handles driver))))
  (is (= "Ministache"
         (:title (window-handle driver))))
  (-> driver
      (find-it {:tag :a, :text "is amazing!"})
      click)
  (is (= "Ministache"
         (:title (window-handle driver))))
  (is (= 2
         (count (window-handles driver))))
  (switch-to-window driver (second (window-handles driver)))
  (is (= (str test-base-url "clojure")
         (:url (window-handle driver))))
  (switch-to-other-window driver)
  (is (= test-base-url
         (:url (window-handle driver))))
  (-> driver
      (switch-to-window (find-window driver {:url (str test-base-url "clojure")})))
  (close driver)
  (is (= test-base-url
         (:url (window-handle driver)))))

(defn wait-until-should-wait-for-condition
  [driver]
  (is (= "Ministache" (title driver)))
  (execute-script driver "setTimeout(function () { window.document.title = \"asdf\"}, 3000)")
  (wait-until driver (fn [d#] (= "asdf" (title d#))))
  (is (= "asdf" (title driver))))

(defn wait-until-should-throw-on-timeout
  [driver]
  (is (thrown? TimeoutException
               (do
                 (execute-script driver "setTimeout(function () { window.document.title = \"test\"}, 6000)")
                 (wait-until driver (fn [d#] (= "test" (title d#))))))))

(defn wait-until-should-allow-timeout-argument
  [driver]
  (is (thrown? TimeoutException
               (do
                 (execute-script driver "setTimeout(function () { window.document.title = \"test\"}, 10000)")
                 (wait-until driver (fn [d#] (= (title d#) "test")) 1000)))))

(defn implicit-wait-should-cause-find-to-wait
  [driver]
  (implicit-wait driver 3000)
  (execute-script driver "setTimeout(function () { window.document.body.innerHTML = \"<div id='test'>hi!</div>\"}, 1000)")
  (is (= "test"
         (attribute (find-element driver (by-id "test")) :id))))

;; Not sure how we'll test that flash in fact flashes,
;; but at least this will catch changing API's
(defn test-flash-helper
  [driver]
  (-> driver
      (find-it {:tag :a, :text "Moustache"})
      flash))

;;; Fixture fn's ;;;
(defn reset-driver
  [driver]
  (to driver test-base-url))

(defn seed-driver-cache
  [driver]
  (cache/seed driver {:url (current-url driver), {:query [:foo]} "bar"}))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;                       ;;;
;;; RUN ACTUAL TESTS HERE ;;;
;;;                       ;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn run-common-tests
  [driver]
  (doseq [common-test [test-browser-basics
                       back-forward-should-traverse-browser-history
                       to-should-open-given-url-in-browser
                       should-be-able-to-find-elements-using-low-level-by-wrappers
                       find-it-should-support-basic-attr-val-map
                       find-it-should-support-regexes-in-attr-val-map
                       find-it-should-support-hierarchical-querying
                       exists-should-return-truthy-falsey-and-should-not-throw-an-exception
                       visible-should-return-truthy-falsey-when-visible
                       present-should-return-truthy-falsey-when-exists-and-visible
                       generated-xpath-should-wrap-strings-in-double-quotes
                       xpath-function-should-return-string-xpath-of-element
                       html-function-should-return-string-html-of-element
                       find-table-cell-should-find-cell-with-coords
                       find-table-row-should-find-all-cells-for-row
                       test-form-elements
                       quick-fill-should-accept-special-seq-and-perform-batch-actions-on-form
                       quick-fill-submit-should-always-return-nil
                       should-be-able-to-toggle-between-open-windows
                       wait-until-should-wait-for-condition
                       wait-until-should-throw-on-timeout
                       wait-until-should-allow-timeout-argument
                       implicit-wait-should-cause-find-to-wait
                       test-flash-helper]]
    (reset-driver driver)
    (seed-driver-cache driver)
    (common-test driver)))

(deftest test-common-features-across-browsers
  (doseq [driver [firefox-driver
                  firefox-driver-no-cache
                  chrome-driver
                  opera-driver]]
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
  ;; assume at test-base-url
  (is (cache/cacheable? firefox-driver (find-it firefox-driver {:tag :a, :class "external"})))
  (is (not (cache/cacheable? firefox-driver {:class "external"})))
  (is (cache/cacheable? firefox-driver {:css "ol#pages"}))
  (is (not (cache/cacheable? firefox-driver :table)))
  (is (not (cache/cacheable? firefox-driver {:css "#pages"}))))

(deftest test-cache-excludes
  ;; includes are tested by default
  (let [temp-dr (to (new-driver {:browser :firefox
                                 :cache-spec {:strategy :basic,
                                              :args [],
                                              :exclude [ (fn [element] (= (attribute element :class) "external")),
                                                         {:css "ol#pages"}]}})
                    test-base-url)]
    (is (cache/cacheable? temp-dr (find-it temp-dr {:tag :table})))
    (is (cache/cacheable? temp-dr (find-it temp-dr {:css "#pages"})))
    (is (not (cache/cacheable? temp-dr (find-it temp-dr {:tag :a, :class "external"}))))
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
  (is (with-browser [tmp-dr (to (new-driver {:browser :firefox
                                             :profile (ff/new-profile)})
                                test-base-url)]
        (log/info "[x] Starting Firefox with custom profile.")
        (driver? tmp-dr))))

(deftest firefox-should-support-extensions
  (is (with-browser [tmp-dr (to (new-driver {:browser :firefox
                                             :profile (doto (ff/new-profile)
                                                        (ff/enable-extension :firebug))})
                                test-base-url)]
        (log/info "[x] Starting Firefox with extensions.")
        (driver? tmp-dr))))
