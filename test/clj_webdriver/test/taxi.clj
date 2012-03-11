(ns clj-webdriver.test.taxi
  (:use clj-webdriver.taxi
        clj-webdriver.test.config
        [clj-webdriver.test.util :only [thrown?]]
        [ring.adapter.jetty :only [run-jetty]]
        [clojure.string :only [lower-case]]
        midje.sweet)
  (:require [clj-webdriver.core :as core]
            [clj-webdriver.test.example-app.core :as web-app])
  (:import [org.openqa.selenium TimeoutException]))

(defonce test-server (run-jetty #'web-app/routes {:port test-port, :join? false}))
(def driver {:browser :chrome})
(defn- go
  ([] (go ""))
  ([path] (to (str test-base-url path))))

;; No fixtures needed because with-driver does setup/teardown

(with-driver driver
  ;; Browser Basics ;;
  (go)
  (facts
   (class clj-webdriver.taxi/*driver*) => clj_webdriver.driver.Driver
   (current-url) => test-base-url
   (title) => "Ministache"
   (page-source) => #"(?i)html>")

  ;; Back, forward ;;
  (go)
  (click (find-element {:tag :a, :text "example form"}))
  (Thread/sleep 500) ;; cross-browser race condition
  (fact (current-url) => (str test-base-url "example-form"))
  (back)
  (fact (current-url) => test-base-url)
  (forward)
  (fact (current-url) => (str test-base-url "example-form"))
  (go "clojure")
  (back 2)
  (fact (current-url) => test-base-url)
  (forward 2)
  (fact (current-url) => (str test-base-url "clojure"))
  
  ;; To function (navigation) ;;
  (to (str test-base-url "example-form"))
  (facts
   (current-url) => (str test-base-url "example-form")
   (title) => "Ministache")

  ;; Cookie handling ;;
  (go)
  (add-cookie {:name "my_cookie" :value "my_cookie_value"})
  (facts
   (count (cookies)) => #(> % 0)
   (:value (cookie "my_cookie")) => "my_cookie_value")
  (delete-cookie "my_cookie")
  (fact (some (fn [c] (= (:name c) "my_cookie")) (cookies)) => falsey)
  (delete-all-cookies)
  (fact (count (cookies)) => zero?)

  ;; Find capability, CSS default ;;
  (go "example-form")
  (facts
   (attribute "#first_name" :id) => "first_name"
   (attribute "input[name='first_name']" :id) => "first_name"
   (text "a") => "home"
   (text "a.menu-item") => "home"
   (text "#footer a.menu-item") => "home"
   (attribute "option[value*='cial_']" :value) => "social_media"
   (attribute "option[value^='social_']" :value) => "social_media"
   (attribute "option[value$='_media']" :value) => "social_media"
   (attribute "option[value]" :value) => "france")
  (go)
  (facts
   (attribute "*.first.odd" :class) => "first odd"
   (attribute (find-element-under (find-element {:tag :li, :text #"simple"})
                                  (core/by-tag :a))
              :href) => "http://clojure.blip.tv/file/4824610/"
   (attribute (find-element-under (find-element {:tag :li, :text #"simple"})
                                  {:tag :a})
              :href) => "http://clojure.blip.tv/file/4824610/"
   (text (nth (elements "a") 1)) => "Moustache"
   (text "*.external") => "Moustache"
   (attribute "*.first.odd" :class) => "first odd"
   (attribute "li.first.odd" :class) => "first odd"
   (count (elements "a")) => 8)

  ;; Advanced CSS flexible selectors and regex support in clj-webdriver.core
  (go "example-form")
  (facts
   (attribute "*[type='text']" :id) => "first_name"
   (attribute "input[type='text']" :id) => "first_name"
   (attribute "input[type='text'][name='first_name']" :id) => "first_name"
   (attribute "input[type='text'][name^='first_']" :id) => "first_name"
   (attribute (find-element {:tag :input, :type "text", :name #"first_"}) :id) => "first_name"
   (attribute (find-element {:tag :input, :type "text", :name #"last_"}) :id) => "last_name"
   (attribute (find-element {:tag :input, :type "text", :name #"last_"}) :value) => "Smith")
  (go)
  (facts
   (text "a[class^='exter']") => "Moustache"
   (text "a.external[href*='github']") => "Moustache"
   (text "a[class*='exter'][href*='github']") => "Moustache"
   (count (elements "*[class*='-item']")) => 3
   (count (elements "a[class*='-item']")) => 3
   (count (elements "a[class*='exter'][href*='github']")) => 2)

  ;; Querying "under" elements
  ;; This is the part that will see more love once #42 is fixed (decouple by-*)
  ;;
  ;; You can either use a by-foo function (in clj-webdriver.core), or a map.
  ;; The map will currently generate a (by-xpath ...) form for you based on the map,
  ;; but it's not as powerful as the core/find-element map syntax (which handles things
  ;; like button*, radio, checkbox, etc.).
  (go)
  (facts
   (text (find-element-under "div#content" (core/by-css "a.external"))) => "Moustache"
   (text (find-element-under "div#content" {:tag :a, :class :external})) => "Moustache"
   (text (find-element-under "div#content" (core/by-css "a[class*='exter']"))) => "Moustache"
   (text (find-element-under "div#content" (core/by-css "a[href*='github']"))) => "Moustache"
   (text (find-element-under "#footer" (core/by-tag :a))) => "home"
   (count (find-elements-under "#footer" (core/by-tag :a))) => 3
   (count (find-elements-under "div#content" (core/by-css "a[class*='exter']"))) => 2)

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
   (text "//a[text()='home']") => "home"
   (text "//a[text()='example form']") => "example form")
  (go)
  (facts
   (attribute "//*[text()='Moustache']" :href) => "https://github.com/cgrand/moustache")

  ;; XPath wrap strings in double quotes
  (go)
  (fact (exists? (find-element {:text "File's Name"})) => truthy))

(.stop test-server)