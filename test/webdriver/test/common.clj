;; Namespace with implementations of test cases
(ns webdriver.test.common
  (:require [clojure.test :refer :all]
            [clojure.string :refer [lower-case]]
            [clojure.java.io :as io]
            [clojure.tools.logging :as log]
            [webdriver.test.helpers :refer :all]
            [webdriver.core :refer :all]
            [webdriver.util :refer :all]
            [webdriver.form :refer :all])
  (:import java.io.File
           [org.openqa.selenium TimeoutException NoAlertPresentException WebDriver]))

;;;;;;;;;;;;;;;;;;;;;;;;
;;;                  ;;;
;;; Test Definitions ;;;
;;;                  ;;;
;;;;;;;;;;;;;;;;;;;;;;;;
(defn browser-basics
  [driver]
  (is (instance? WebDriver driver))
  (is (= *base-url* (current-url driver)))
  (is (= "Ministache" (title driver)))
  (is (boolean (re-find #"(?i)html>" (page-source driver)))))

(defn back-forward-should-traverse-browser-history
  [driver]
  (-> driver
      (find-element {:tag :a, :text "example form"})
      click)
  (wait-until driver (fn [d] (= (str *base-url* "example-form") (current-url d))))
  (is (= (str *base-url* "example-form") (current-url driver)))
  (back driver)
  (is (= *base-url* (current-url driver)))
  (forward driver)
  (is (= (str *base-url* "example-form") (current-url driver))))

(defn to-should-open-given-url-in-browser
  [driver]
  (to driver (str *base-url* "example-form"))
  (is (= (str *base-url* "example-form") (current-url driver)))
  (is (= "Ministache" (title driver))))

(defn should-be-able-to-find-element-bys-using-low-level-by-wrappers
  [driver]
  (-> driver
      (find-element {:tag :a, :text "example form"})
      click)
  (wait-until driver (fn [d] (immortal (find-element-by d (by-id "first_name")))))
  (is (= "first_name"
         (attribute (find-element-by driver (by-id "first_name")) :id)))
  (is (= "home"
         (text (find-element-by driver (by-link-text "home")))))
  (is (= "example form"
         (text (find-element-by driver (by-partial-link-text "example")))))
  (is (= "first_name"
         (attribute (find-element-by driver (by-name "first_name")) :id)))
  (is (= "home"
         (text (find-element-by driver (by-tag "a")))))
  (is (= "home"
         (text (find-element-by driver (by-xpath "//a[text()='home']")))))
  (is (= "home"
         (text (find-element-by driver (by-class-name "menu-item")))))
  (is (= "home"
         (text (find-element-by driver (by-css-selector "#footer a.menu-item")))))
  (is (= "social_media"
         (attribute (find-element-by driver (by-attr-contains :option :value "cial_")) :value)))
  (is (= "social_media"
         (attribute (find-element-by driver (by-attr-starts :option :value "social_")) :value)))
  (is (= "social_media"
         (attribute (find-element-by driver (by-attr-ends :option :value "_media")) :value)))
  (is (= "france"
         (attribute (find-element-by driver (by-has-attr :option :value)) :value)))
  (to driver *base-url*)
  (is (= "first odd"
         (attribute (find-element-by driver (by-class-name "first odd")) :class))))

(defn find-element-should-support-basic-attr-val-map
  [driver]
  (is (= "Moustache"
         (text (nth (find-elements driver {:tag :a}) 1))))
  (is (= "Moustache"
         (text (find-element driver {:class "external"}))))
  (is (= "first odd"
         (attribute (find-element driver {:class "first odd"}) :class)))
  (is (= "first odd"
         (attribute (find-element driver {:tag :li, :class "first odd"}) :class)))
  (is (= "https://github.com/cgrand/moustache"
         (attribute (find-element driver {:text "Moustache"}) "href")))
  (is (= 10
         (count (find-elements driver {:tag :a}))))
  (-> driver
      (find-element {:tag :a, :text "example form"})
      click)
  (wait-until driver (fn [d] (immortal (find-element d {:type "text"}))))
  (is (= "first_name"
         (attribute (find-element driver {:type "text"}) "id")))
  (is (= "first_name"
         (attribute (find-element driver {:tag :input, :type "text"}) "id")))
  (is (= "first_name"
         (attribute (find-element driver {:tag :input, :type "text", :name "first_name"}) "id"))))

(defn find-element-should-support-hierarchical-querying
  [driver]
  (is (= "Moustache"
         (text (find-element driver [{:tag :div, :id "content"}, {:tag :a, :class "external"}]))))
  (is (= "home"
         (text (find-element driver [{:tag :*, :id "footer"}, {:tag :a}]))))
  (is (= 5
         (count (find-elements driver [{:tag :*, :id "footer"}, {:tag :a}])))))

(defn hierarchical-querying-should-not-support-css-or-xpath-attrs
  [driver]
  (is (thrown? IllegalArgumentException
               (find-element driver [{:tag :div, :id "content", :css "div#content"}, {:tag :a, :class "external"}])))
  (is (thrown? IllegalArgumentException
               (find-element driver [{:tag :div, :id "content", :xpath "//div[@id='content']"}, {:tag :a, :class "external"}]))))

(defn exists-should-return-truthy-falsey-and-should-not-throw-an-exception
  [driver]
  (is (-> driver
          (find-element {:tag :a})
          exists?))
  (is (not
       (-> driver
           (find-element {:tag :area})
           exists?))))

(defn visible-should-return-truthy-falsey-when-visible
  [driver]
  (is (-> driver
          (find-element {:tag :a, :text "Moustache"})
          visible?))
  (is (not
       (-> driver
           (find-element {:tag :a, :href "#pages"})
           visible?)))
  (is (-> driver
          (find-element {:tag :a, :text "Moustache"})
          displayed?))
  (is (not
       (-> driver
           (find-element {:tag :a, :href "#pages"})
           displayed?))))

(defn present-should-return-truthy-falsey-when-exists-and-visible
  [driver]
  (is (-> driver
          (find-element {:tag :a, :text "Moustache"})
          present?))
  (is (not
       (-> driver
           (find-element {:tag :a, :href "#pages"})
           present?))))

(defn drag-and-drop-by-pixels-should-work
  [driver]
  (-> driver
      (find-element {:tag :a, :text "javascript playground"})
      click)
  ;; Just check to make sure this page still has the element we expect,
  ;; since it's an external site
  (wait-until driver (fn [d] (immortal (find-element d {:id "draggable"}))))
  (is (-> driver
          (find-element {:id "draggable"})
          present?))
  (let [el-to-drag (find-element driver {:id "draggable"})
        {o-x :x o-y :y} (location-in-viewport el-to-drag)
        {n-x :x n-y :y} (do
                          (drag-and-drop-by driver el-to-drag {:x 20 :y 20})
                          (location-in-viewport el-to-drag))
        x-diff (Math/abs (- n-x o-x))
        y-diff (Math/abs (- n-y o-y))]
    (is (= x-diff 20))
    (is (= y-diff 20))))

(defn drag-and-drop-on-elements-should-work
  [driver]
  (-> driver
      (find-element {:tag :a, :text "javascript playground"})
      click)
  ;; Just check to make sure this page still has the element we expect,
  ;; since it's an external site
  (wait-until driver (fn [d] (immortal (find-element d {:id "draggable"}))))
  (is (-> driver
          (find-element {:id "draggable"})
          present?))
  (is (-> driver
          (find-element {:id "droppable"})
          present?))
  (let [draggable (find-element driver {:id "draggable"})
        droppable (find-element driver {:id "droppable"})
        {o-x :x o-y :y} (location-in-viewport draggable)
        {n-x :x n-y :y} (do
                          (drag-and-drop driver draggable droppable)
                          (location-in-viewport draggable))]
    (is (or (not= o-x n-x)
            (not= o-y n-y)))
    (is (re-find #"ui-state-highlight" (attribute droppable :class)))))

(defn should-be-able-to-determine-if-elements-intersect-each-other
  [driver]
  (click (find-element driver {:tag :a, :text "example form"}))
  (wait-until driver (fn [d] (immortal (find-element d {:id "first_name"}))))
  (is (intersects? (find-element driver {:id "first_name"})
                   (find-element driver {:id "personal-info-wrapper"})))
  (is (not
       (intersects? (find-element driver {:id "first_name"})
                    (find-element driver {:id "last_name"})))))

;; Default wrap for strings is double quotes
(defn generated-xpath-should-wrap-strings-in-double-quotes
  [driver]
  (is (find-element driver {:text "File's Name"})))

(defn xpath-function-should-return-string-xpath-of-element
  [driver]
  (is (= (xpath (find-element driver {:tag :a, :text "Moustache"})) "/html/body/div[2]/div/p/a")))

(defn html-function-should-return-string-html-of-element
  [driver]
  (is (re-find #"href=\"https://github\.com/cgrand/moustache\"" (html (find-element driver {:tag :a, :text "Moustache"})))))

(defn find-table-cell-should-find-cell-with-coords
  [driver]
  (is (= "th"
         (lower-case (tag (find-table-cell driver
                                           (find-element driver {:id "pages-table"})
                                           [0 0])))))
  (is (= "th"
         (lower-case (tag (find-table-cell driver
                                           (find-element driver {:id "pages-table"})
                                           [0 1])))))
  (is (= "td"
         (lower-case (tag (find-table-cell driver
                                           (find-element driver {:id "pages-table"})
                                           [1 0])))))
  (is (= "td"
         (lower-case (tag (find-table-cell driver
                                           (find-element driver {:id "pages-table"})
                                           [1 1]))))))

(defn find-table-row-should-find-all-cells-for-row
  [driver]
  (is (= 2
         (count (find-table-row driver
                                (find-element driver {:id "pages-table"})
                                0))))
  (is (= "th"
         (lower-case (tag (first (find-table-row driver
                                                   (find-element driver {:id "pages-table"})
                                                   0))))))
  (is (= "td"
         (lower-case (tag (first (find-table-row driver
                                                   (find-element driver {:id "pages-table"})
                                                   1)))))))

(defn form-elements
  [driver]
  (to driver (str *base-url* "example-form"))
  ;; Clear element
  ;; (-> driver
  ;;     (find-element [{:tag :form, :id "example_form"}, {:tag :input, :name #"last_"}])
  ;;     clear)
  ;; (is (= ""
  ;;        (value (find-element driver [{:tag :form, :id "example_form"}, {:tag :input, :name #"last_"}]))))
  ;; Radio buttons
  (is (= true
         (selected? (find-element driver {:tag :input, :type "radio", :value "male"}))))
  (-> driver
      (find-element {:tag :input, :type "radio", :value "female"})
      select)
  (is (= true
         (selected? (find-element driver {:tag :input, :type "radio", :value "female"}))))
  (-> driver
      (find-element {:tag :radio, :value "male"})
      select)
  (is (= true
         (selected? (find-element driver {:tag :input, :type "radio", :value "male"}))))
  ;; Checkboxes
  ;; (is (= false
  ;;        (selected? (find-element driver {:tag :input, :type "checkbox", :name #"(?i)clojure"}))))
  ;; (-> driver
  ;;     (find-element {:tag :input, :type "checkbox", :name #"(?i)clojure"})
  ;;     toggle)
  ;; (is (= true
  ;;        (selected? (find-element driver {:tag :input, :type "checkbox", :name #"(?i)clojure"}))))
  ;; (-> driver
  ;;     (find-element {:tag :checkbox, :name #"(?i)clojure"})
  ;;     click)
  ;; (is (= false
  ;;        (selected? (find-element driver {:tag :input, :type "checkbox", :name #"(?i)clojure"}))))
  ;; (-> driver
  ;;     (find-element {:tag :checkbox, :type "checkbox", :name #"(?i)clojure"})
  ;;     select)
  ;; (is (= true
  ;;        (selected? (find-element driver {:tag :input, :type "checkbox", :name #"(?i)clojure"}))))
  ;; Text fields
  (println (current-url driver))
  (-> driver
      (find-element {:tag :input, :id "first_name"})
      (input-text "foobar"))
  (is (= "foobar"
         (value (find-element driver {:tag :input, :id "first_name"}))))
  (-> driver
      (find-element {:tag :textfield, :id "first_name"})
      clear
      (input-text "clojurian"))
  (is (= "clojurian"
         (value (find-element driver {:tag :textfield, :id "first_name"}))))
  ;; Boolean attributes (disabled, readonly, etc)
  (is (= "disabled"
         (attribute (find-element driver {:id "disabled_field"}) :disabled)))
  (is (= "readonly"
         (attribute (find-element driver {:id "purpose_here"}) :readonly)))
  (is (nil?
       (attribute (find-element driver {:id "disabled_field"}) :readonly)))
  (is (nil?
       (attribute (find-element driver {:id "purpose_here"}) :disabled)))
  ;; Buttons
  ;; (is (= 4
  ;;        (count (find-elements driver {:tag :button*}))))
  ;; (is (= 1
  ;;        (count (find-elements driver {:tag :button*, :class "button-button"}))))
  ;; (is (= 1
  ;;        (count (find-elements driver {:tag :button*, :id "input-input-button"}))))
  ;; (is (= 1
  ;;        (count (find-elements driver {:tag :button*, :class "input-submit-button"}))))
  ;; (is (= 1
  ;;        (count (find-elements driver {:tag :button*, :class "input-reset-button"}))))
  )

(defn select-element-functions-should-behave-as-expected
  [driver]
  (to driver (str *base-url* "example-form"))
  (let [select-el (find-element driver {:tag "select", :id "countries"})]
    (is (= 4
           (count (all-options select-el))))
    (is (= 1
           (count (all-selected-options select-el))))
    (is (= "bharat"
           (attribute (first-selected-option select-el) :value)))
    (is (= "bharat"
           (attribute (first (all-selected-options select-el)) :value)))
    (is (false?
         (multiple? select-el)))
    (select-option select-el
                   {:value "deutschland"})
    (is (= 1
           (count (all-selected-options select-el))))
    (is (= "deutschland"
           (attribute (first-selected-option select-el) :value)))
    (is (= "deutschland"
           (attribute (first (all-selected-options select-el)) :value)))
    (select-by-index select-el
                     0)
    (is (= 1
           (count (all-selected-options select-el))))
    (is (= "france"
           (attribute (first-selected-option select-el) :value)))
    (is (= "france"
           (attribute (first (all-selected-options select-el)) :value)))
    (select-by-text select-el
                    "Haiti")
    (is (= 1
           (count (all-selected-options select-el))))
    (is (= "ayiti"
           (attribute (first-selected-option select-el) :value)))
    (is (= "ayiti"
           (attribute (first (all-selected-options select-el)) :value)))
    (select-by-value select-el
                     "bharat")
    (is (= 1
           (count (all-selected-options select-el))))
    (is (= "bharat"
           (attribute (first-selected-option select-el) :value)))
    (is (= "bharat"
           (attribute (first (all-selected-options select-el)) :value))))
  (let [select-el (find-element driver {:tag "select", :id "site_types"})]
    (is (true?
         (multiple? select-el)))
    (is (= 4
           (count (all-options select-el))))
    (is (zero?
         (count (all-selected-options select-el))))
    (select-option select-el {:index 0})
    (is (= 1
           (count (all-selected-options select-el))))
    (is (= "blog"
           (attribute (first-selected-option select-el) :value)))
    (is (= "blog"
           (attribute (first (all-selected-options select-el)) :value)))
    (select-option select-el {:value "social_media"})
    (is (= 2
           (count (all-selected-options select-el))))
    (is (= "social_media"
           (attribute (second (all-selected-options select-el)) :value)))
    (deselect-option select-el {:index 0})
    (is (= 1
           (count (all-selected-options select-el))))
    (is (= "social_media"
           (attribute (first-selected-option select-el) :value)))
    (is (= "social_media"
           (attribute (first (all-selected-options select-el)) :value)))
    (select-option select-el {:value "search_engine"})
    (is (= 2
           (count (all-selected-options select-el))))
    (is (= "search_engine"
           (attribute (second (all-selected-options select-el)) :value)))
    (deselect-by-index select-el 1)
    (is (= 1
           (count (all-selected-options select-el))))
    (is (= "search_engine"
           (attribute (first-selected-option select-el) :value)))
    (is (= "search_engine"
           (attribute (first (all-selected-options select-el)) :value)))
    (select-option select-el {:value "code"})
    (is (= 2
           (count (all-selected-options select-el))))
    (is (= "code"
           (attribute (last (all-selected-options select-el)) :value)))
    (deselect-by-text select-el "Search Engine")
    (is (= 1
           (count (all-selected-options select-el))))
    (is (= "code"
           (attribute (first-selected-option select-el) :value)))
    (select-all select-el)
    (is (= 4
           (count (all-selected-options select-el))))
    (deselect-all select-el)
    (is (zero?
         (count (all-selected-options select-el))))))

(defn quick-fill-should-accept-special-seq-and-perform-batch-actions-on-form
  [driver]
  (to driver (str *base-url* "example-form"))
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
         (value (find-element driver {:tag :input, :id "first_name"}))))
  (is (= "Hickey"
         (value (find-element driver {:tag :input, :id "last_name"}))))
  (is (= "Creator of Clojure"
         (value (find-element driver {:tag :textarea, :name "bio"}))))
  (is (selected?
       (find-element driver {:tag :input, :type "radio", :value "female"})))
  (is (selected?
       (find-element driver {:tag :option, :value "france"}))))

(defn quick-fill-submit-should-always-return-nil
  [driver]
  (to driver (str *base-url* "example-form"))
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
  (let [window-1 (window-handle driver)]
    (is (= (count (window-handles driver))
           1))
    (-> driver
        (find-element {:tag :a, :text "is amazing!"})
        click)
    (wait-until driver (fn [d] (immortal (= "Ministache" (title d)))))
    (let [window-2 (first (disj (window-handles driver) window-1))]
      (is (= (count (window-handles driver))
             2))
      (is (not= window-1 window-2))
      (is (= (title driver)
             "Ministache"))
      (switch-to-window driver window-2)
      (is (= (str *base-url* "clojure")
             (current-url driver)))
      (switch-to-other-window driver)
      (is (= *base-url* (current-url driver)))
      (switch-to-other-window driver)
      (close driver)
      (switch-to-window driver (first (window-handles driver)))
      (= *base-url* (current-url driver)))))

(defn alert-dialog-handling
  [driver]
  (click (find-element driver {:text "example form"}))
  (wait-until driver (fn [d] (immortal (find-element d {:tag :button}))))
  (let [act (fn [] (click (find-element driver {:tag :button})))]
    (act)
    (is (alert-obj driver) "No alert dialog could be located")
    (accept driver)
    (is (thrown? NoAlertPresentException
                 (alert-obj driver)))
    (act)
    (is (= (alert-text driver)
           "Testing alerts."))
    (dismiss driver)
    (is (thrown? NoAlertPresentException
                 (alert-obj driver)))))

(defn wait-until-should-wait-for-condition
  [driver]
  (is (= "Ministache" (title driver)))
  (execute-script driver "setTimeout(function () { window.document.title = \"asdf\"}, 2000)")
  (wait-until driver (fn [d] (= (title d) "asdf")))
  (is (= (title driver) "asdf")))

(defn wait-until-should-throw-on-timeout
  [driver]
  (is (thrown? TimeoutException
               (do
                 (execute-script driver "setTimeout(function () { window.document.title = \"test\"}, 6000)")
                 (wait-until driver (fn [d] (= "test" (title d))))))))

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
         (attribute (find-element-by driver (by-id "test")) :id))))

;; This behavior is so inconsistent, I'm almost inclined to take it out
;; of clj-webdriver entirely.
;;
;; (defn frames-by-index
;;   [driver]
;;   (to driver "http://selenium.googlecode.com/svn/trunk/docs/api/java/index.html")
;;   (is (= (count (find-elements driver {:tag :frame})) 3))
;;   (switch-to-frame driver 0)
;;   (is (exclusive-between (count (find-elements driver {:tag :a}))
;;                          50 100))
;;   (switch-to-default driver)
;;   (switch-to-frame driver 1)
;;   (is (exclusive-between (count (find-elements driver {:tag :a}))
;;                          370 400))
;;   (switch-to-default driver)
;;   (switch-to-frame driver 2)
;;   (is (exclusive-between (count (find-elements driver {:tag :a}))
;;                          30 50)))

(defn frames-by-element
  [driver]
  ;; TODO Implement page with frames
  (to driver "http://selenium.googlecode.com/svn/trunk/docs/api/java/index.html")
  (is (= (count (find-elements driver {:tag :frame})) 3))
  (switch-to-frame driver (find-element driver {:name "packageListFrame"}))
  (is (exclusive-between (count (find-elements driver {:tag :a}))
                         30 50))
  (switch-to-default driver)
  (switch-to-frame driver (find-element driver {:name "packageFrame"}))
  (is (exclusive-between (count (find-elements driver {:tag :a}))
                         370 400))
  (switch-to-default driver)
  (switch-to-frame driver (find-element driver {:name "classFrame"}))
  (is (exclusive-between (count (find-elements driver {:tag :a}))
                         50 100)))

;; Not sure how we'll test that flash in fact flashes,
;; but at least this exercises the function call.
(defn flash-helper
  [driver]
  (-> driver
      (find-element {:tag :a, :text "Moustache"})
      flash))

(def tmpdir (let [td (System/getProperty "java.io.tmpdir")]
              (if (.endsWith td File/separator)
                td
                (str td File/separator))))
(def screenshot-file (str tmpdir "screenshot_test.png"))
(defn take-screenshot
  [driver]
  (is (string? (get-screenshot driver :base64)))
  (is (> (count (get-screenshot driver :bytes)) 0))
  (is (= (class (get-screenshot driver :file)) java.io.File))
  (is (= (class (get-screenshot driver :file screenshot-file)) java.io.File))
  ;; the following will throw an exception if deletion fails, hence our test
  (io/delete-file screenshot-file))

;;; Fixture fn's ;;;
(defn reset-driver
  [driver]
  (to driver *base-url*))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;                       ;;;
;;; RUN ACTUAL TESTS HERE ;;;
;;;                       ;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(def common-tests [#'browser-basics
                   #'back-forward-should-traverse-browser-history
                   #'to-should-open-given-url-in-browser
                   #'should-be-able-to-find-element-bys-using-low-level-by-wrappers
                   #'find-element-should-support-basic-attr-val-map
                   #'find-element-should-support-hierarchical-querying
                   #'hierarchical-querying-should-not-support-css-or-xpath-attrs
                   #'exists-should-return-truthy-falsey-and-should-not-throw-an-exception
                   #'visible-should-return-truthy-falsey-when-visible
                   #'present-should-return-truthy-falsey-when-exists-and-visible
                   #'drag-and-drop-by-pixels-should-work
                   #'drag-and-drop-on-elements-should-work
                   #'should-be-able-to-determine-if-elements-intersect-each-other
                   #'generated-xpath-should-wrap-strings-in-double-quotes
                   #'xpath-function-should-return-string-xpath-of-element
                   #'html-function-should-return-string-html-of-element
                   #'find-table-cell-should-find-cell-with-coords
                   #'find-table-row-should-find-all-cells-for-row
                   #'form-elements
                   #'select-element-functions-should-behave-as-expected
                   #'quick-fill-should-accept-special-seq-and-perform-batch-actions-on-form
                   #'quick-fill-submit-should-always-return-nil
                   #'should-be-able-to-toggle-between-open-windows
                   #'wait-until-should-wait-for-condition
                   #'wait-until-should-throw-on-timeout
                   #'wait-until-should-allow-timeout-argument
                   #'implicit-wait-should-cause-find-to-wait
                   #'frames-by-element
                   #'flash-helper
                   #'take-screenshot])

(defn defcommontests*
  ([prefix driver] (defcommontests* prefix webdriver.test.helpers/*base-url* driver))
  ([prefix url driver]
   (let [test-names (mapv #(symbol (str prefix (:name (meta %)))) common-tests)
         ts (mapv (fn [v n]
                    `(deftest ~n
                       (binding [webdriver.test.helpers/*base-url* ~url]
                         (~v ~driver))))
                  common-tests test-names)]
     `(do
        ~@ts))))

(defmacro defcommontests
  "This can be run from within another namespace to have this suite defined and run there. This gives a separate deftest for each test, which makes it much easier to understand what broke, and run individual tests within those namespaces more easily."
  [& args]
  (apply defcommontests* args))

(def alert-tests [#'alert-dialog-handling])
