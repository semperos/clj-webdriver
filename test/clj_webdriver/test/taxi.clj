(ns clj-webdriver.test.taxi
  (:use clojure.test
        clj-webdriver.taxi
        [clj-webdriver.test.config :only [test-base-url]]
        [clj-webdriver.test.util :only [start-server]]
        [clj-webdriver.test.util :only [thrown?]]
        [clojure.string :only [lower-case]]
        ;; midje.sweet
        )
  (:require [clj-webdriver.core :as core]
            [clj-webdriver.test.example-app.core :as web-app])
  (:import [org.openqa.selenium TimeoutException]))

(defn start-browser-fixture
  [f]
  (set-driver! {:browser :chrome})
  (f))

(defn reset-browser-fixture
  [f]
  (to test-base-url)
  (f))

(defn quit-browser-fixture
  [f]
  (f)
  (quit))

(use-fixtures :once start-server start-browser-fixture quit-browser-fixture)
(use-fixtures :each reset-browser-fixture)

;; RUN TESTS HERE
(deftest test-browser-basics
  (is (= (class clj-webdriver.taxi/*driver*) clj_webdriver.driver.Driver))
  (is (= (current-url) test-base-url))
  (is (= (title) "Ministache"))
  (is (re-find #"(?i)html>" (page-source))))

(deftest back-forward-should-traverse-browser-history
  (click (find-element {:tag :a, :text "example form"}))
  (is (= (current-url) (str test-base-url "example-form")))
  (back)
  (is (= (current-url) (str test-base-url)))
  (forward)
  (is (= (current-url) (str test-base-url "example-form")))
  (click (find-element {:tag :a, :text "clojure"}))
  (back 2)
  (is (= (current-url) (str test-base-url)))
  (forward 2)
  (is (= (current-url) (str test-base-url "clojure"))))

(deftest to-should-open-given-url-in-browser
  (to (str test-base-url "example-form"))
  (is (= (current-url) (str test-base-url "example-form")))
  (is (= (title) "Ministache")))

(deftest test-cookie-handling
  (add-cookie {:name "my_cookie" :value "my_cookie_value"})
  (is (> (count (cookies)) 0))
  (is (= (:value (cookie "my_cookie")) "my_cookie_value"))
  (delete-cookie "my_cookie")
  (is (not (some (fn [c] (= (:name c) "my_cookie")) (cookies))))
  (delete-all-cookies)
  (is (zero? (count (cookies)))))

(deftest test-finding-elements-with-CSS
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
  (is (= (count (elements "a")) 8))
  (is (= (text "a[class^='exter']") "Moustache"))
  (is (= (text "a.external[href*='github']") "Moustache"))
  (is (= (text "a[class*='exter'][href*='github']") "Moustache"))
  (is (= (count (elements "*[class*='-item']")) 3))
  (is (= (count (elements "a[class*='-item']")) 3))
  (is (= (count (elements "a[class*='exter'][href*='github']")) 2)))

(deftest test-querying-under-elements
  ;; Querying "under" elements
  ;; This is the part that will see more love once #42 is fixed (decouple by-*)
  ;;
  ;; You can either use a by-foo function (in clj-webdriver.core), or a map.
  ;; The map will currently generate a (by-xpath ...) form for you based on the map,
  ;; but it's not as powerful as the core/find-element map syntax (which handles things
  ;; like button*, radio, checkbox, etc.).
  (is (= (text (find-element-under "div#content" (core/by-css "a.external"))) "Moustache")))

(comment
 (with-driver driver
   
   (go)
   (facts
     => "Moustache"
    (text (find-element-under "div#content" {:css "a.external"})) => "Moustache"
    (text (find-element-under "div#content" {:tag :a, :class "external"})) => "Moustache"
    (text (find-element-under "div#content" {:css "a[class*='exter']"})) => "Moustache"
    (text (find-element-under "div#content" {:css "a[href*='github']"})) => "Moustache"
    (text (find-element-under "#footer" (core/by-tag :a))) => "home"
    (count (find-elements-under "#footer" {:tag :a})) => 3
    (count (find-elements-under "div#content" {:css "a[class*='exter']"})) => 2)

   ;; Exists/visible/present ;;
   (go)
   (facts
    (exists? "a") => truthy
    (exists? "area") => falsey
    (exists? "a[href='#pages']") => truthy
    (visible? "a.external") => truthy
    (visible? "a[href='#pages']") => falsey
    (displayed? "a.external") => truthy
    (displayed? "a[href='#pages']") => falsey
    (present? "a.external") => truthy
    (present? "a[href='#pages']") => falsey)

   ;; Drag and drop
   (to "http://jqueryui.com/demos/draggable")
   (fact
     ;; since it's an external site, make sure that draggable div is still there
     (present? "#draggable"))
   (let [el-to-drag (element {:id "draggable"})
         {o-x :x o-y :y} (location el-to-drag)
         {n-x :x n-y :y} (do
                           (drag-and-drop-by el-to-drag {:x 20 :y 20})
                           (location el-to-drag))
         x-diff (Math/abs (- n-x o-x))
         y-diff (Math/abs (- n-y o-y))]
     (facts
      (= x-diff 20)
      (= y-diff 20)))

   ;; Element Intersection
   (go "example-form")
   (facts
    (intersect? "#first_name" "#personal-info-wrapper") => truthy
    (intersect? "#first_name" "#last_name") => falsey)

   ;; XPath generation
   (go)
   (fact (xpath "a.external") => "/html/body/div[2]/div/p/a")

   ;; HTML of an element (inner)
   (go)
   (fact (html "a.external") => #"href=\"https://github\.com/cgrand/moustache\"")

   ;; Table cell finding
   (go)
   (facts
    (lower-case (tag (find-table-cell "#pages-table" [0 0]))) => "th"
    (lower-case (tag (find-table-cell "#pages-table" [0 1]))) => "th"
    (lower-case (tag (find-table-cell "#pages-table" [1 0]))) => "td"
    (lower-case (tag (find-table-cell "#pages-table" [1 1]))) => "td")

   ;; Table row finding
   (go)
   (facts
    (count (find-table-row "#pages-table" 0)) => 2
    (lower-case (tag (first (find-table-row "#pages-table" 0)))) => "th"
    (lower-case (tag (first (find-table-row "#pages-table" 1)))) => "td")

   ;; Form elements ;;
   (go "example-form")

   ;; Clear
   (clear "form#example_form input[id^='last_']")
   (fact (value "form#example_form input[id^='last_']") => empty?)

   ;; Radio buttons
   (fact (selected? "input[type='radio'][value='male']") => truthy)
   (select "input[type='radio'][value='female']")
   (fact (selected? "input[type='radio'][value='female']") => truthy)
   (select "input[type='radio'][value='male']")
   (fact (selected? "input[type='radio'][value='male']") => truthy)
   (fact (selected? "input[type='radio'][value='female']") => falsey)

   ;; Checkboxes
   (fact (selected? "input[type='checkbox'][name*='clojure']") => falsey)
   (toggle "input[type='checkbox'][name*='clojure']")
   (fact (selected? "input[type='checkbox'][name*='clojure']") => truthy)
   (click "input[type='checkbox'][name*='clojure']")
   (fact (selected? "input[type='checkbox'][name*='clojure']") => falsey)
   (select "input[type='checkbox'][name*='clojure']")
   (fact (selected? "input[type='checkbox'][name*='clojure']") => truthy)

   ;; Text fields
   (input-text "input#first_name" "foobar")
   (fact (value "input#first_name") => "foobar")
   (-> "input#first_name"
       clear
       (input-text "clojurian"))
   (fact (value "input#first_name") => "clojurian")

   ;; "Boolean" attributes (e.g., readonly="readonly")
   (facts
    (attribute "#disabled_field" :disabled) => "disabled"
    (attribute "#purpose_here" :readonly) => "readonly"
    (attribute "#disabled_field" :readonly) => nil?
    (attribute "#purpose_here" :disabled) => nil?)

   ;; Select Lists
   (let [q "select#countries"]
     (facts
      (count (options q)) => 4
      (count (selected-options q)) => 1
      (attribute (first (selected-options q)) :value) => "bharat"
      (multiple? q) => falsey)
     (select-option q {:value "deutschland"})
     (facts
      (count (selected-options q)) => 1
      (attribute (first (selected-options q)) :value) => "deutschland")
     (select-by-index q 0)
     (facts
      (count (selected-options q)) => 1
      (attribute (first (selected-options q)) :value) => "france")
     (select-by-text q "Haiti")
     (facts
      (count (selected-options q)) => 1
      (attribute (first (selected-options q)) :value) => "ayiti")
     (select-by-value q "bharat")
     (facts
      (count (selected-options q)) => 1
      (attribute (first (selected-options q)) :value) => "bharat"))
   (let [q "select#site_types"]
     (facts
      (multiple? q) => truthy
      (count (options q)) => 4
      (count (selected-options q)) => zero?)
     (select-option q {:index 0})
     (facts
      (count (selected-options q)) => 1
      (attribute (first (selected-options q)) :value) => "blog")
     (select-option q {:value "social_media"})
     (facts
      (count (selected-options q)) => 2
      (attribute (second (selected-options q)) :value) => "social_media")
     (deselect-option q {:index 0})
     (facts
      (count (selected-options q)) => 1
      (attribute (first (selected-options q)) :value) => "social_media")
     (select-option q {:value "search_engine"})
     (facts
      (count (selected-options q)) => 2
      (attribute (second (selected-options q)) :value) => "search_engine")
     (deselect-by-index q 1)
     (facts
      (count (selected-options q)) => 1
      (attribute (first (selected-options q)) :value) => "search_engine")
     (select-option q {:value "code"})
     (facts
      (count (selected-options q)) => 2
      (attribute (last (selected-options q)) :value) => "code")
     (deselect-by-text q "Search Engine")
     (facts
      (count (selected-options q)) => 1
      (attribute (first (selected-options q)) :value) => "code")
     (select-all q)
     (fact (count (selected-options q)) => 4)
     (deselect-all q)
     (fact (count (selected-options q)) => zero?))

   ;; Quick-fill ;;
   (go "example-form")
   (facts
    (count (quick-fill {"#first_name" clear}
                       {"#first_name" "Richard"}
                       {"#last_name" clear}
                       {"#last_name" "Hickey"}
                       {"*[name='bio']" clear}
                       {"*[name='bio']" #(input-text % "Creator of Clojure")}
                       {"input[type='radio'][value='female']" click}
                       {"select#countries" #(select-by-value % "france")})) => 8
    (count (distinct
            (quick-fill {"#first_name" clear}
                        {"#first_name" "Richard"}
                        {"#last_name" clear}
                        {"#last_name" "Hickey"}
                        {"*[name='bio']" clear}
                        {"*[name='bio']" #(input-text % "Creator of Clojure")}
                        {"input[type='radio'][value='female']" click}
                        {"select#countries" #(select-by-value % "france")}))) => 5
    (value "input#first_name") => "Richard"
    (value "input#last_name") => "Hickey"
    (value "textarea[name='bio']") => "Creator of Clojure"
    (selected? "input[type='radio'][value='female']") => truthy
    (selected? "option[value='france']") => truthy
    (quick-fill-submit {"#first_name" clear}
                       {"#first_name" "Richard"}
                       {"#last_name" clear}
                       {"#last_name" "Hickey"}
                       {"*[name='bio']" clear}
                       {"*[name='bio']" #(input-text % "Creator of Clojure")}
                       {"input[type='radio'][value='female']" click}
                       {"select#countries" #(select-by-value % "france")}) => nil?)

   ;; Window Handling ;;
   (go)
   (facts
    (count (window-handles)) => 1
    (:title (window-handle)) => "Ministache")
   (click "a[target='_blank'][href*='clojure']")
   (facts
    (:title (window-handle)) => "Ministache"
    (count (window-handles)) => 2)
   (switch-to-window (second (window-handles)))
   (fact (:url (window-handle)) => (str test-base-url "clojure"))
   (switch-to-other-window)
   (fact (:url (window-handle)) => test-base-url)
   (switch-to-window (find-window {:url (str test-base-url "clojure")}))
   (close)
   (fact (:url (window-handle)) => test-base-url)

   ;; Wait functionality ;;
   (fact (title) => "Ministache")
   (execute-script "setTimeout(function () { window.document.title = \"asdf\"}, 3000)")
   (wait-until #(= (title) "asdf"))

   (fact (thrown? TimeoutException
                  (do
                    (execute-script "setTimeout(function () { window.document.title = \"test\"}, 6000)")
                    (wait-until #(= (title) "test")))))
   (fact (thrown? TimeoutException
                  (do
                    (execute-script "setTimeout(function () { window.document.title = \"test\"}, 6000)")
                    (wait-until #(= (title) "test") 1000))))

   ;; Implicit Wait ;;
   (go)
   (implicit-wait 3000)
   (execute-script "setTimeout(function () { window.document.body.innerHTML = \"<div id='test'>hi!</div>\"}, 1000)")
   (fact (attribute "#test" :id) => "test")

   ;; Flash ;;
   (go)
   (fact (attribute (flash "a.external") :class) => "external"))

 ;; Find capabilitiy, XPath default
 (with-driver-fn driver xpath-finder
   (go "example-form")
   (facts
    (text {:tag :a, :text "home"}) => "home"
    (text "//a[text()='home']") => "home"
    (text "//a[text()='example form']") => "example form")
   (go)
   (facts
    (attribute "//*[text()='Moustache']" :href) => "https://github.com/cgrand/moustache")

   ;; XPath wrap strings in double quotes
   (go)
   (fact (exists? (find-element {:text "File's Name"})) => truthy))

)