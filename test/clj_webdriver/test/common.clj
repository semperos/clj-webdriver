;; Namespace with implementations of test cases
(ns clj-webdriver.test.common
  (:use clojure.test
        [clj-webdriver core util window-handle wait options form-helpers]
        [clj-webdriver.test.util :only [thrown?]]
        [clj-webdriver.test.config :only [test-base-url]])
  (:require [clj-webdriver.cache :as cache]
            [clojure.tools.logging :as log])
  (:import [clj_webdriver.driver.Driver]
           [org.openqa.selenium TimeoutException]))

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
  (Thread/sleep 500) ;; race condition issue with OperaDriver (on my machine, at least)
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

(defn hierarchical-querying-should-not-support-css-or-xpath-attrs
  [driver]
  (is (thrown? IllegalArgumentException
               (find-it driver [{:tag :div, :id "content", :css "div#content"}, {:tag :a, :class "external"}])))
  (is (thrown? IllegalArgumentException
               (find-it driver [{:tag :div, :id "content", :xpath "//div[@id='content']"}, {:tag :a, :class "external"}]))))

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

(defn select-element-functions-should-behave-as-expected
  [driver]
  (to driver (str test-base-url "example-form"))
  (is (= 4
         (count (all-options (find-it driver {:tag "select"})))))
  (is (= 1
         (count (all-selected-options (find-it driver {:tag "select"})))))
  (is (= "bharat"
         (attribute (first-selected-option (find-it driver {:tag "select"})) :value)))
  (is (= "bharat"
         (attribute (first (all-selected-options (find-it driver {:tag "select"}))) :value))))

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
                       hierarchical-querying-should-not-support-css-or-xpath-attrs
                       exists-should-return-truthy-falsey-and-should-not-throw-an-exception
                       visible-should-return-truthy-falsey-when-visible
                       present-should-return-truthy-falsey-when-exists-and-visible
                       generated-xpath-should-wrap-strings-in-double-quotes
                       xpath-function-should-return-string-xpath-of-element
                       html-function-should-return-string-html-of-element
                       find-table-cell-should-find-cell-with-coords
                       find-table-row-should-find-all-cells-for-row
                       test-form-elements
                       select-element-functions-should-behave-as-expected
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