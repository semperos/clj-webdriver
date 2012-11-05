(ns clj-webdriver.test.taxi
  (:use clojure.test
        clj-webdriver.taxi
        [clj-webdriver.test.config :only [test-base-url]]
        [clj-webdriver.test.util :only [deftest-template-param start-server exclusive-between thrown?]]
        [clojure.string :only [lower-case]])
  (:require [clj-webdriver.core :as core]
            [clj-webdriver.test.example-app.core :as web-app])
  (:import [org.openqa.selenium TimeoutException NoAlertPresentException]))

;; Alternate driver (not used with Taxi's set-driver!)
(def alt-driver (atom nil))

(defn start-browser-fixture
  [f]
  (set-driver! {:browser :firefox})
  (reset! alt-driver (core/new-driver {:browser :firefox}))
  (f))

(defn reset-browser-fixture
  [f]
  (to test-base-url)
  (to @alt-driver test-base-url)
  (f))

(defn quit-browser-fixture
  [f]
  (f)
  (quit)
  (quit @alt-driver))

(use-fixtures :once start-server start-browser-fixture quit-browser-fixture)
(use-fixtures :each reset-browser-fixture)

;; RUN TESTS HERE
(deftest-template-param
  test-browser-basics @alt-driver
  (is (= (class *driver*) clj_webdriver.driver.Driver))
  (is (= (current-url __) test-base-url))
  (is (= (title __) "Ministache"))
  (is (re-find #"(?i)html>" (page-source __))))

(deftest-template-param
  back-forward-should-traverse-browser-history @alt-driver
  (click __ (find-element __ {:tag :a, :text "example form"}))
  (is (= (current-url __) (str test-base-url "example-form")))
  (back __)
  (is (= (current-url __) test-base-url))
  (forward __)
  (is (= (current-url __) (str test-base-url "example-form")))
  (click __ (find-element __ {:tag :a, :text "clojure"}))
  (back __ 2)
  (is (= (current-url __) (str test-base-url)))
  (forward __ 2)
  (is (= (current-url __) (str test-base-url "clojure"))))

(deftest-template-param
  to-should-open-given-url-in-browser @alt-driver
  (to (str test-base-url "example-form"))
  (is (= (current-url) (str test-base-url "example-form")))
  (is (= (title) "Ministache")))

(deftest-template-param
  test-cookie-handling @alt-driver
  (add-cookie {:name "my_cookie" :value "my_cookie_value"})
  (is (> (count (cookies)) 0))
  (is (= (:value (cookie "my_cookie")) "my_cookie_value"))
  (delete-cookie "my_cookie")
  (is (not (some (fn [c] (= (:name c) "my_cookie")) (cookies))))
  (delete-all-cookies)
  (is (zero? (count (cookies)))))

(deftest-template-param
  test-finding-elements-with-CSS @alt-driver
  (click (find-element {:tag :a, :text "example form"}))
  (is (= (attribute "#first_name" :id) "first_name"))
  (is (= (attribute "*[type='text']" :id) "first_name"))
  (is (= (attribute "input[name='first_name']" :id) "first_name"))
  (is (= (attribute "input[type='text'][name='first_name']" :id) "first_name"))
  (is (= (attribute "input[type='text'][name^='first_']" :id) "first_name"))
  (is (= (text "a") "home"))
  (is (= (text "a.menu-item") "home"))
  (is (= (attribute {:tag "a", :class "menu-item"} :class) "menu-item"))
  (is (= (text "#footer a.menu-item") "home"))
  (is (= (attribute "option[value*='cial_']" :value) "social_media"))
  (is (= (attribute "option[value^='social_']" :value) "social_media"))
  (is (= (attribute "option[value$='_media']" :value) "social_media"))
  (is (= (attribute "option[value]" :value) "france"))
  (back) ;; on starting page
  (is (= (attribute "*.first.odd" :class) "first odd"))
  (is (= (tag {:class "external"}) "a"))
  (is (= (text (nth (elements "a") 1)) "Moustache"))
  (is (= (text "*.external") "Moustache"))
  (is (= (attribute "*.first.odd" :class) "first odd"))
  (is (= (attribute "li.first.odd" :class) "first odd"))
  (is (= (count (elements "a")) 10))
  (is (= (text "a[class^='exter']") "Moustache"))
  (is (= (text "a.external[href*='github']") "Moustache"))
  (is (= (text "a[class*='exter'][href*='github']") "Moustache"))
  (is (= (count (elements "*[class*='-item']")) 5))
  (is (= (count (elements "a[class*='-item']")) 5))
  (is (= (count (elements "a[class*='exter'][href*='github']")) 2)))

(deftest-template-param
  test-querying-under-elements @alt-driver
  (is (= (text (find-element-under "div#content" (core/by-css "a.external"))) "Moustache"))
  (is (= (text (find-element-under "div#content" {:css "a.external"})) "Moustache"))
  (is (= (text (find-element-under "div#content" {:tag :a, :class "external"})) "Moustache"))
  (is (= (text (find-element-under "div#content" {:css "a[class*='exter']"})) "Moustache"))
  (is (= (text (find-element-under "div#content" {:css "a[href*='github']"})) "Moustache"))
  (is (= (text (find-element-under "#footer" (core/by-tag :a))) "home"))
  (is (= (count (find-elements-under "#footer" {:tag :a})) 5))
  (is (= (count (find-elements-under "div#content" {:css "a[class*='exter']"})) 2)))

(deftest-template-param
  text-exists-visible-present @alt-driver
  (is (exists? "a"))
  (is (not (exists? "area")))
  (is (exists? "a[href='#pages']"))
  (is (visible? "a.external"))
  (is (not (visible? "a[href='#pages']")))
  (is (displayed? "a.external"))
  (is (not (displayed? "a[href='#pages']")))
  (is (present? "a.external"))
  (is (not (present? "a[href='#pages']"))))

(deftest-template-param
  test-drag-and-drop @alt-driver
  (click (find-element {:tag :a, :text "javascript playground"}))
  (is (present? "#draggable"))
  (let [el-to-drag (element {:id "draggable"})
        {o-x :x o-y :y} (location el-to-drag)
        {n-x :x n-y :y} (do
                          (drag-and-drop-by el-to-drag {:x 20 :y 20})
                          (location el-to-drag))
        x-diff (Math/abs (- n-x o-x))
        y-diff (Math/abs (- n-y o-y))]
    (is (= x-diff 20))
    (is (= y-diff 20))))

(deftest-template-param
  drag-and-drop-on-elements-should-work @alt-driver
  (click (find-element {:tag :a, :text "javascript playground"}))
  ;; Just check to make sure this page still has the element we expect,
  ;; since it's an external site
  (is (present? "#draggable"))
  (is (present? "#droppable"))
  (let [draggable (element "#draggable")
        droppable (element "#droppable")
        {o-x :x o-y :y} (location draggable)
        {n-x :x n-y :y} (do
                          (drag-and-drop draggable droppable)
                          (location draggable))]
    (is (or (not= o-x n-x)
            (not= o-y n-y)))
    (is (re-find #"ui-state-highlight" (attribute droppable :class)))))

(deftest-template-param
  test-element-intersection @alt-driver
  (click (find-element {:tag :a, :text "example form"}))
  (is (intersects? "#first_name" "#personal-info-wrapper"))
  (is (not (intersects? "#first_name" "#last_name"))))

(deftest-template-param
  test-xpath-output @alt-driver
  (is (= (xpath "a.external") "/html/body/div[2]/div/p/a")))

(deftest-template-param
  test-html-output @alt-driver
  (is (re-find #"href=\"https://github\.com/cgrand/moustache\"" (html "a.external"))))

(deftest-template-param
  test-table-finding @alt-driver
  (is (= (current-url) test-base-url))
  (is (exists? "#pages-table"))
  (is (= (lower-case (tag (find-table-cell "#pages-table" [0 0]))) "th"))
  (is (= (lower-case (tag (find-table-cell "#pages-table" [0 1]))) "th"))
  (is (= (lower-case (tag (find-table-cell "#pages-table" [1 0]))) "td"))
  (is (= (lower-case (tag (find-table-cell "#pages-table" [1 1]))) "td"))
  (is (= (count (find-table-row "#pages-table" 0)) 2))
  (is (= (lower-case (tag (first (find-table-row (element "#pages-table") 0)))) "th"))
  (is (= (lower-case (tag (first (find-table-row (element"#pages-table") 1)))) "td")))

(deftest-template-param
  form-elements @alt-driver
  (click (find-element {:tag :a :text, "example form"}))
  ;; Clear
  (clear "form#example_form input[id^='last_']")
  (is (empty? (value "form#example_form input[id^='last_']")))
  ;; Radio buttons
  (is (selected? "input[type='radio'][value='male']"))
  (select "input[type='radio'][value='female']")
  (is (selected? "input[type='radio'][value='female']"))
  (select "input[type='radio'][value='male']")
  (is (selected? "input[type='radio'][value='male']"))
  (is (not (selected? "input[type='radio'][value='female']")))
  ;; Checkboxes
  (is (not (selected? "input[type='checkbox'][name*='clojure']")))
  (toggle "input[type='checkbox'][name*='clojure']")
  (is (selected? "input[type='checkbox'][name*='clojure']"))
  (click "input[type='checkbox'][name*='clojure']")
  (is (not (selected? "input[type='checkbox'][name*='clojure']")))
  (select "input[type='checkbox'][name*='clojure']")
  (is (selected? "input[type='checkbox'][name*='clojure']"))
  ;; Text fields
  (input-text "input#first_name" "foobar")
  (is (= (value "input#first_name") "foobar"))
  (clear "input#first_name")
  (input-text "input#first_name" "clojurian")
  (is (= (value "input#first_name") "clojurian"))
  ;; "Boolean" element attributes (e.g., readonly="readonly")
  (is (= (attribute "#disabled_field" :disabled) "disabled"))
  (is (= (attribute "#purpose_here" :readonly) "readonly"))
  (is (nil? (attribute "#disabled_field" :readonly)))
  (is (nil? (attribute "#purpose_here" :disabled)))
  ;; Select lists
  (let [q "select#countries"]
    (is (= (count (options q)) 4))
    (is (= (count (selected-options q)) 1))
    (is (= (attribute (first (selected-options q)) :value) "bharat"))
    (is (not (multiple? q)))
    (select-option q {:value "deutschland"})
    (is (= (count (selected-options q)) 1))
    (is (= (attribute (first (selected-options q)) :value) "deutschland"))
    (select-by-index q 0)
    (is (= (count (selected-options q)) 1))
    (is (= (attribute (first (selected-options q)) :value) "france"))
    (select-by-text q "Haiti")
    (is (= (count (selected-options q)) 1))
    (is (= (attribute (first (selected-options q)) :value) "ayiti"))
    (select-by-value q "bharat")
    (is (= (count (selected-options q)) 1))
    (is (= (attribute (first (selected-options q)) :value) "bharat")))
  (let [q "select#site_types"]
    (is (multiple? q))
    (is (= (count (options q)) 4))
    (is (zero? (count (selected-options q))))
    (select-option q {:index 0})
    (is (= (count (selected-options q)) 1))
    (is (= (attribute (first (selected-options q)) :value) "blog"))
    (select-option q {:value "social_media"})
    (is (= (count (selected-options q)) 2))
    (is (= (attribute (second (selected-options q)) :value) "social_media"))
    (deselect-option q {:index 0})
    (is (= (count (selected-options q)) 1))
    (attribute (first (selected-options q)) :value) "social_media"
    (select-option q {:value "search_engine"})
    (is (= (count (selected-options q)) 2))
    (is (= (attribute (second (selected-options q)) :value) "search_engine"))
    (deselect-by-index q 1)
    (is (= (count (selected-options q)) 1))
    (is (= (attribute (first (selected-options q)) :value) "search_engine"))
    (select-option q {:value "code"})
    (is (= (count (selected-options q)) 2))
    (is (= (attribute (last (selected-options q)) :value) "code"))
    (deselect-by-text q "Search Engine")
    (is (= (count (selected-options q)) 1))
    (is (= (attribute (first (selected-options q)) :value) "code"))
    (select-all q)
    (is (= (count (selected-options q)) 4))
    (deselect-all q)
    (is (zero? (count (selected-options q))))))

(deftest-template-param
  test-quickfill @alt-driver
  (click (find-element {:tag :a :text, "example form"}))
  (is (= (count (quick-fill
                            {"#first_name" clear}
                            {"#first_name" "Richard"}
                            {"#last_name" clear}
                            {"#last_name" "Hickey"}
                            {"*[name='bio']" clear}
                            {"*[name='bio']" #(input-text % "Creator of Clojure")}
                            {"input[type='radio'][value='female']" click}
                            {"select#countries" #(select-by-value % "france")})) 8))
  (is (= (count (distinct
            (quick-fill
                        {"#first_name" clear}
                        {"#first_name" "Richard"}
                        {"#last_name" clear}
                        {"#last_name" "Hickey"}
                        {"*[name='bio']" clear}
                        {"*[name='bio']" #(input-text % "Creator of Clojure")}
                        {"input[type='radio'][value='female']" click}
                        {"select#countries" #(select-by-value % "france")}))) 5))
  (is (= (value "input#first_name") "Richard"))
  (is (= (value "input#last_name") "Hickey"))
  (is (= (value "textarea[name='bio']") "Creator of Clojure"))
  (is (selected? "input[type='radio'][value='female']"))
  (is (selected? "option[value='france']"))
  (is (nil? (quick-fill-submit
                               {"#first_name" clear}
                               {"#first_name" "Richard"}
                               {"#last_name" clear}
                               {"#last_name" "Hickey"}
                               {"*[name='bio']" clear}
                               {"*[name='bio']" #(input-text % "Creator of Clojure")}
                               {"input[type='radio'][value='female']" click}
                               {"select#countries" #(select-by-value % "france")}))))

(deftest-template-param
  test-window-handling @alt-driver
  (is (= (count (windows)) 1))
  (is (= (:title (window)) "Ministache"))
  (click "a[target='_blank'][href*='clojure']")
  (is (= (:title (window)) "Ministache"))
  (is (= (count (windows)) 2))
  (switch-to-window (second (windows)))
  (is (= (:url (window)) (str test-base-url "clojure")))
  (switch-to-other-window)
  (is (= (:url (window)) test-base-url))
  (switch-to-window (find-window {:url (str test-base-url "clojure")}))
  (close)
  (is (= (:url (window)) test-base-url)))

(deftest-template-param
  test-waiting-until @alt-driver
  (is (= (title) "Ministache"))
  (execute-script "setTimeout(function () { window.document.title = \"asdf\"}, 3000)")
  (wait-until #(= (title) "asdf") 5000 0)
  (is (thrown? TimeoutException
                  (do
                    (execute-script "setTimeout(function () { window.document.title = \"test\"}, 6000)")
                    (wait-until #(= (title) "test") 5000 0))))
  (is (not (thrown? TimeoutException
                    (do
                      (execute-script "setTimeout(function () { window.document.title = \"test\"}, 7000)")
                      (wait-until #(= (title) "test") 1000 0))))))

(deftest-template-param
  test-implicit-wait @alt-driver
  (implicit-wait 3000)
  (execute-script "setTimeout(function () { window.document.body.innerHTML = \"<div id='test'>hi!</div>\"}, 1000)")
  (is (= (attribute "#test" :id) "test")))

;; For some reason this test behaves differently than the one below
;; and the one in clj-webdriver.test.common. Even the count of frame elements
;; is different at page start. Disabling for now, as this usage is not
;; as cross-browser compatible or reliable.
;;
;; (deftest-template-param test-frames-by-index
;;   (to "http://selenium.googlecode.com/svn/trunk/docs/api/java/index.html")
;;   (is (= (count (elements "frame")) 3))
;;   (switch-to-frame 0)
;;   (is (exclusive-between (count (elements "a")) 30 50))
;;   (switch-to-default)
;;   (switch-to-frame 1)
;;   (is (exclusive-between (count (elements "a")) 370 400))
;;   (switch-to-default)
;;   (switch-to-frame 2)
;;   (is (exclusive-between (count (elements "a")) 50 100)))

(deftest-template-param
  test-frames-by-element @alt-driver
  (to "http://selenium.googlecode.com/svn/trunk/docs/api/java/index.html")
  (is (= (count (elements "frame")) 3))
  (switch-to-frame "frame[name='packageListFrame']")
  (is (exclusive-between (count (elements "a")) 30 50))
  (switch-to-default)
  (switch-to-frame "frame[name='packageFrame']")
  (is (exclusive-between (count (elements "a")) 370 400))
  (switch-to-default)
  (switch-to-frame "frame[name='classFrame']")
  (is (exclusive-between (count (elements "a")) 50 100)))

(deftest-template-param
  test-flash @alt-driver
  (is (= (attribute (flash "a.external") :class) "external")))

(deftest-template-param
  test-using-xpath-instead-of-css @alt-driver
  (set-finder! xpath-finder)
  (click (find-element {:tag :a :text, "example form"}))
  (is (= (text {:tag :a, :text "home"}) "home"))
  (is (= (text "//a[text()='home']") "home"))
  (is (= (text "//a[text()='example form']") "example form"))
  (back) ;; starting page
  (is (= (attribute "//*[text()='Moustache']" :href) "https://github.com/cgrand/moustache"))
  (is (exists? (find-element {:text "File's Name"}))))

(deftest-template-param
  test-alert-dialog-handling @alt-driver
  (click (find-element {:tag :a, :text "example form"}))
  (let [act (fn [] (click "button"))]
    (act)    
    (is (alert-obj) "No alert dialog could be located")
    (accept)
    (is (thrown? NoAlertPresentException
                 (alert-obj)))
    (act)
    (is (= (alert-text)
           "Testing alerts."))
    (dismiss)
    (is (thrown? NoAlertPresentException
                 (alert-obj)))))

(deftest-template-param
  test-window-size @alt-driver
  (let [orig-size (window-size)
        small {:width 500 :height 400}
        large {:width 1024 :height 800}]
    (window-resize small)
    (is (= (window-size) small))
    (window-resize large)
    (is (= (window-size) large))
    (window-resize orig-size)
    (is (= (window-size) orig-size))))

(deftest-template-param
  test-window-resize-with-one-dimension @alt-driver
  (let [orig-size (window-size)
        small {:height 400}
        large {:width 1024}]
    (window-resize small)
    (is (= (:width (window-size)) (:width orig-size)))
    (window-resize orig-size)
    (is (= (window-size) orig-size))
    (window-resize large)
    (is (= (:height (window-size)) (:height orig-size)))))

(deftest-template-param
  test-window-position @alt-driver
  (let [origin (window-position)
        new-position {:x 100 :y 245}]
    (window-reposition new-position)
    (is (= (window-position) new-position))
    (window-reposition origin)
    (is (= (window-position) origin))))

(deftest-template-param
  test-window-reposition-with-one-coordinate @alt-driver
  (let [origin (window-position)
        position-y {:y 245}
        position-x {:x 100}]
    (window-reposition position-y)
    (is (= (:x (window-position)) (:x origin)))
    (window-reposition origin)
    (is (= (window-position) origin))
    (window-reposition position-x)
    (is (= (:y (window-position)) (:y origin)))))

(deftest-template-param
  test-window-maximizing @alt-driver
  (let [orig-size (window-size (window-resize {:width 300 :height 300}))
        max-size (window-size (window-maximize))]
    (is (> (:width max-size) (:width orig-size)))
    (is (> (:height max-size) (:height orig-size)))))