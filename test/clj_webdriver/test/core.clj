(ns clj-webdriver.test.core
  (:use [clj-webdriver core util window-handle wait options form-helpers]
        [clj-webdriver.driver :only [get-cache driver?]])
  (:use [ring.adapter.jetty :only [run-jetty]]
        clojure.test)  
  (:require [clj-webdriver.test.example-app.core :as web-app]
            [clj-webdriver.cache :as cache]
            [clj-webdriver.firefox :as ff]
            [clojure.tools.logging :as log]
            [clojure.java.io :as jio])
  (:import [clj_webdriver.driver.Driver]
           [org.openqa.selenium TimeoutException]))

;; CI
(defn travis?
  "Return true if running on Travis server"
  []
  (= (System/getenv "TRAVIS") "true"))

;; ## Setup ##
(def test-port 5744)
(def test-host "localhost")
(def test-base-url (str "http://" test-host ":" test-port "/"))

(def dr (to (new-driver {:browser :firefox
                         :cache-spec {:strategy :basic,
                                      :args [],
                                      :include [ (fn [element] (= (attribute element :class) "external"))
                                                 {:css "ol#pages"}]}})
            test-base-url))
(def dr-plain (to (new-driver {:browser :firefox}) test-base-url))

(defn start-server [f]
  (loop [server (run-jetty #'web-app/routes {:port test-port, :join? false})]
    (if (.isStarted server)
      (do
        (f)
        (.stop server))
      (recur server))))

(defn reset-browser-fixture
  [f]
  (to dr test-base-url)
  (to dr-plain test-base-url)
  (f))

(defn quit-browser-fixture
  [f]
  (f)
  (quit dr)
  (quit dr-plain))

(defn seed-driver-cache
  [f]
  (cache/seed dr {:url (current-url dr), {:query [:foo]} "bar"})
  (f))

(use-fixtures :once start-server quit-browser-fixture)
(use-fixtures :each reset-browser-fixture seed-driver-cache)



;; ## Cache-Based Tests ##
(deftest test-browser-basics
  (is (= clj_webdriver.driver.Driver (class dr)))
  (is (= test-base-url (current-url dr)))
  (is (= "Ministache" (title dr)))
  (is (boolean (re-find #"(?i)<!DOCTYPE html>" (page-source dr)))))

(deftest test-back-forward
  (-> dr
      (find-it {:tag :a, :text "example form"})
      click)
  (is (= (str test-base-url "example-form") (current-url dr)))
  (back dr)
  (is (= test-base-url (current-url dr)))
  (forward dr)
  (is (= (str test-base-url "example-form") (current-url dr))))

(deftest test-to
  (to dr (str test-base-url "example-form"))
  (is (= (str test-base-url "example-form") (current-url dr)))
  (is (= "Ministache" (title dr))))

(deftest test-bys
  (-> dr
      (find-it {:tag :a, :text "example form"})
      click)
  (is (= "first_name"
         (attribute (find-element dr (by-id "first_name")) :id)))
  (is (= "home"
         (text (find-element dr (by-link-text "home")))))
  (is (= "example form"
         (text (find-element dr (by-partial-link-text "example")))))
  (is (= "first_name"
         (attribute (find-element dr (by-name "first_name")) :id)))
  (is (= "home"
         (text (find-element dr (by-tag "a")))))
  (is (= "home"
         (text (find-element dr (by-xpath "//a[text()='home']")))))
  (is (= "home"
         (text (find-element dr (by-class-name "menu-item")))))
  (is (= "home"
         (text (find-element dr (by-css-selector "#footer a.menu-item"))))))

(deftest test-find*
  (is (= "Moustache"
         (text (nth (find-them dr {:tag :a}) 1))))
  (is (= "Moustache"
         (text (find-it dr {:class "external"}))))
  (is (= "https://github.com/cgrand/moustache"
         (attribute (find-it dr {:text "Moustache"}) "href")))
  (is (= "Moustache"
         (text (find-it dr {:tag :a, :class #"exter"}))))
  (is (= "Moustache"
         (text (find-it dr {:tag :a, :text #"Mous"}))))
  (is (= "Moustache"
         (text (find-it dr {:tag :a, :class "external", :href #"github"}))))
  (is (= "Moustache"
         (text (find-it dr {:tag :a, :class #"exter", :href #"github"}))))
  (is (= "Moustache"
         (text (find-it dr [:div {:id "content"}, :a {:class "external"}]))))
  (is (= "Moustache"
         (text (find-it dr [:div {:id "content"}, :a {:class #"exter"}]))))
  (is (= "Moustache"
         (text (find-it dr [:div {:id "content"}, :a {:href #"github"}]))))
  (is (= "home"
         (text (find-it dr [:* {:id "footer"}, :a {}]))))
  (is (= 8
         (count (find-them dr {:tag :a}))))
  (is (= 3
         (count (find-them dr {:class #"-item"}))))
  (is (= 3
         (count (find-them dr {:tag :a, :class #"-item"}))))
  (is (= 1
         (count (find-them dr {:tag :a, :text #"hom"}))))
  (is (= 1
         (count (find-them dr {:tag :a, :text #"(?i)HOM"}))))
  (is (= 2
         (count (find-them dr {:tag :a, :class #"exter", :href #"github"}))))
  (is (= 3
         (count (find-them dr [:* {:id "footer"}, :a {}]))))
  (is (= 2
         (count (find-them dr [:div {:id "content"}, :a {:class #"exter"}]))))
  (-> dr
      (find-it {:tag :a, :text "example form"})
      click)
  (is (= "first_name"
         (attribute (find-it dr {:type "text"}) "id")))
  (is (= "first_name"
         (attribute (find-it dr {:tag :input, :type "text"}) "id")))
  (is (= "first_name"
         (attribute (find-it dr {:tag :input, :type "text", :name "first_name"}) "id")))
  (is (= "first_name"
         (attribute (find-it dr {:tag :input, :type "text", :name #"first_"}) "id")))
  (is (= "last_name"
         (attribute (find-it dr {:tag :input, :type "text", :name #"last_"}) "id")))
  (is (= "Smith"
         (attribute (find-it dr {:tag :input, :type "text", :name #"last_"}) "value")))
  (is (= "Smith"
         (attribute (find-it dr {:tag :input, :type "text", :name #"last_"}) "value")))
  (is (= "Smith"
         (attribute (find-it dr [:div {:id "content"}, :input {:name #"last_"}]) "value")))
  (back dr) ;; get back to home page
  (is (-> dr
        (find-it {:tag :a})
        exists?))
  (is (not
       (-> dr
           (find-it {:tag :area})
           exists?)))
  (is (not
       (-> dr
           (find-it {:tag :area})
           exists?)))
  (is (-> dr 
          (find-it {:tag :a, :text "Moustache"})
          visible?))
  (is (-> dr 
          (find-it {:tag :a, :text "Moustache"})
          displayed?))
  (is (-> dr
          (find-it {:tag :a, :text "Moustache"})
          present?))
  (is (not
       (-> dr
           (find-it {:tag :a})
           visible?)))
  (is (not
       (-> dr 
           (find-it {:tag :a})
           displayed?)))
  (is (not
       (-> dr
           (find-it {:tag :a})
           present?))))

;; Default wrap for strings is double quotes
(deftest test-xpath-quote-handling
  (is (find-it dr {:text "File's Name"})))

(deftest text-js-based-fns
  (is (= (xpath (find-it dr {:tag :a, :text "Moustache"})) "/html/body/div[2]/div/p/a"))
  (is (= (html (find-it dr {:tag :a, :text "Moustache"})) "<a xmlns=\"http://www.w3.org/1999/xhtml\" class=\"external\" href=\"https://github.com/cgrand/moustache\">Moustache</a>")))

(deftest test-form-elements
  (to dr (str test-base-url "example-form"))
  ;; Clear element
  (-> dr
      (find-it [:form {:id "example_form"}, :input {:name #"last_"}])
      clear)
  (is (= ""
         (value (find-it dr [:form {:id "example_form"}, :input {:name #"last_"}]))))
  ;; Radio buttons
  (is (= true
         (selected? (find-it dr {:tag :input, :type "radio", :value "male"}))))
  (-> dr
      (find-it {:tag :input, :type "radio", :value "female"})
      select)
  (is (= true
         (selected? (find-it dr {:tag :input, :type "radio", :value "female"}))))
  (-> dr
      (find-it {:tag :radio, :value "male"})
      select)
  (is (= true
         (selected? (find-it dr {:tag :input, :type "radio", :value "male"}))))
  ;; Checkboxes
  (is (= false
         (selected? (find-it dr {:tag :input, :type "checkbox", :name #"(?i)clojure"}))))
  (-> dr
      (find-it {:tag :input, :type "checkbox", :name #"(?i)clojure"})
      toggle)
  (is (= true
         (selected? (find-it dr {:tag :input, :type "checkbox", :name #"(?i)clojure"}))))
  (-> dr
      (find-it {:tag :checkbox, :name #"(?i)clojure"})
      click)
  (is (= false
         (selected? (find-it dr {:tag :input, :type "checkbox", :name #"(?i)clojure"}))))
  (-> dr
      (find-it {:tag :checkbox, :type "checkbox", :name #"(?i)clojure"})
      select)
  (is (= true
         (selected? (find-it dr {:tag :input, :type "checkbox", :name #"(?i)clojure"}))))
  ;; Text fields
  (is (= "true"
         (attribute (find-it dr {:tag :input, :type "text", :value "Testing!"})
                    "readonly")))
  (-> dr
      (find-it {:tag :input, :id "first_name"})
      (input-text "foobar"))
  (is (= "foobar"
         (value (find-it dr {:tag :input, :id "first_name"}))))
  (-> dr
      (find-it {:tag :textfield, :id "first_name"})
      clear
      (input-text "clojurian"))
  (is (= "clojurian"
         (value (find-it dr {:tag :textfield, :id "first_name"})))))

(deftest test-form-helpers
  (to dr (str test-base-url "example-form"))
  (quick-fill dr
              [{"first_name" clear}
               {"first_name" "Richard"}
               {{:id "last_name"} clear}
               {{:id "last_name"} "Hickey"}
               {{:name "bio"} clear}
               {{:name "bio"} #(input-text % "Creator of Clojure")}
               {{:tag "input", :type "radio", :value "female"} click}
               {{:css "select#countries"} #(select-by-value % "france")}])
  (is (= "Richard"
         (value (find-it dr {:tag :input, :id "first_name"}))))
  (is (= "Hickey"
         (value (find-it dr {:tag :input, :id "last_name"}))))
  (is (= "Creator of Clojure"
         (value (find-it dr {:tag :textarea, :name "bio"}))))
  (is (selected?
       (find-it dr {:tag :input, :type "radio", :value "female"})))
  (is (selected?
       (find-it dr {:tag :option, :value "france"}))))

(deftest test-window-handling
  (is (= 1
         (count (window-handles dr))))
  (is (= "Ministache"
         (:title (window-handle dr))))
  (-> dr
      (find-it {:tag :a, :text "is amazing!"})
      click)
  (is (= "Ministache"
         (:title (window-handle dr))))
  (is (= 2
         (count (window-handles dr))))
  (switch-to-window dr (second (window-handles dr)))
  (is (= (str test-base-url "clojure")
         (:url (window-handle dr))))
  (switch-to-other-window dr)
  (is (= test-base-url
         (:url (window-handle dr))))
  (-> dr
      (switch-to-window (find-window dr {:url (str test-base-url "clojure")})))
  (close dr)
  (is (= test-base-url
         (:url (window-handle dr)))))

(deftest wait-until-should-wait-for-condition
  (is (= "Ministache" (title dr)))
  (execute-script dr "setTimeout(function () { window.document.title = \"asdf\"}, 3000)")
  (wait-until dr (fn [d] (= "asdf" (title d))))
  (is (= "asdf" (title dr))))

(deftest wait-until-should-throw-on-timeout
  (is (thrown? TimeoutException
               (do
                 (execute-script dr "setTimeout(function () { window.document.title = \"test\"}, 6000)")
                 (wait-until dr (fn [d] (= "test" (title d))))))))

(deftest wait-until-should-allow-timeout-argument
  (is (thrown? TimeoutException
               (do
                   (execute-script dr "setTimeout(function () { window.document.title = \"test\"}, 10000)")
                   (wait-until dr (fn [d] (= (title d) "test")) 1000)))))

(deftest implicit-wait-should-cause-find-to-wait
  (implicit-wait dr 3000)
  (execute-script dr "setTimeout(function () { window.document.body.innerHTML = \"<div id='test'>hi!</div>\"}, 1000)")
  (is (= "test"
         (attribute (find-element dr (by-id "test")) :id))))

;; Not sure how we'll test that flash in fact flashes,
;; but at least this will catch changing API's
(deftest test-flash-helper
  (-> dr
      (find-it {:tag :a, :text "Moustache"})
      flash))

;; Caching
(deftest test-cache-initialization
  (is (cache/cache-enabled? dr)))

(deftest test-cache-insert
  ;; insert was used to seed the data in the test fixture; test now for presence
  (is (= (get @(get-cache dr) {:query [:foo]}) "bar"))
  (is (nil? (get @(get-cache dr) :wowza))))

(deftest test-in-cache?
  (is (cache/in-cache? dr {:query [:foo]}))
  (is (not (cache/in-cache? dr :wowza))))

(deftest test-cache-retrieve
  (is (= (cache/retrieve dr :foo) "bar"))
  (is (nil? (cache/retrieve dr :wowza))))

(deftest test-cache-delete
  (cache/insert dr {:query [:alpha]} "beta")
  (is (= (cache/retrieve dr :alpha) "beta"))
  (cache/delete dr :alpha)
  (is (nil? (cache/retrieve dr :alpha))))

(deftest test-cache-seed
  (cache/seed dr {{:query [:foo]} "clojure"})
  (is (= (cache/retrieve dr :foo) "clojure"))
  (cache/seed dr)
  (is (= @(get-cache dr) {:url (current-url dr)})))

(deftest test-cacheable?
  ;; assume at test-base-url
  (is (cache/cacheable? dr (find-it dr {:tag :a, :class "external"})))
  (is (not (cache/cacheable? dr {:class "external"})))
  (is (cache/cacheable? dr {:css "ol#pages"}))
  (is (not (cache/cacheable? dr :table)))
  (is (not (cache/cacheable? dr {:css "#pages"}))))

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
  (is (string? (get-screenshot dr :base64)))
  (is (> (count (get-screenshot dr :bytes)) 0))
  (is (= (class (get-screenshot dr :file)) java.io.File))
  (is (= (class (get-screenshot dr :file "/tmp/screenshot_test.png")) java.io.File))
  ;; the following will throw an exception if deletion fails, hence our test
  (jio/delete-file "/tmp/screenshot_test.png"))


;; ## Tests (sans cache) ##
(deftest plain-test-browser-basics
  (is (= clj_webdriver.driver.Driver (class dr-plain)))
  (is (= test-base-url (current-url dr-plain)))
  (is (= "Ministache" (title dr-plain)))
  (is (boolean (re-find #"(?i)<!DOCTYPE html>" (page-source dr-plain)))))

(deftest plain-test-back-forward
  (-> dr-plain
      (find-it {:tag :a, :text "example form"})
      click)
  (is (= (str test-base-url "example-form") (current-url dr-plain)))
  (back dr-plain)
  (is (= test-base-url (current-url dr-plain)))
  (forward dr-plain)
  (is (= (str test-base-url "example-form") (current-url dr-plain))))

(deftest plain-test-to
  (to dr-plain (str test-base-url "example-form"))
  (is (= (str test-base-url "example-form") (current-url dr-plain)))
  (is (= "Ministache" (title dr-plain))))

(deftest plain-test-bys
  (-> dr-plain
      (find-it {:tag :a, :text "example form"})
      click)
  (is (= "first_name"
         (attribute (find-element dr-plain (by-id "first_name")) :id)))
  (is (= "home"
         (text (find-element dr-plain (by-link-text "home")))))
  (is (= "example form"
         (text (find-element dr-plain (by-partial-link-text "example")))))
  (is (= "first_name"
         (attribute (find-element dr-plain (by-name "first_name")) :id)))
  (is (= "home"
         (text (find-element dr-plain (by-tag "a")))))
  (is (= "home"
         (text (find-element dr-plain (by-xpath "//a[text()='home']")))))
  (is (= "home"
         (text (find-element dr-plain (by-class-name "menu-item")))))
  (is (= "home"
         (text (find-element dr-plain (by-css-selector "#footer a.menu-item"))))))

(deftest plain-test-find*
  (is (= "Moustache"
         (text (nth (find-them dr-plain {:tag :a}) 1))))
  (is (= "Moustache"
         (text (find-it dr-plain {:class "external"}))))
  (is (= "https://github.com/cgrand/moustache"
         (attribute (find-it dr-plain {:text "Moustache"}) "href")))
  (is (= "Moustache"
         (text (find-it dr-plain {:tag :a, :class #"exter"}))))
  (is (= "Moustache"
         (text (find-it dr-plain {:tag :a, :text #"Mous"}))))
  (is (= "Moustache"
         (text (find-it dr-plain {:tag :a, :class "external", :href #"github"}))))
  (is (= "Moustache"
         (text (find-it dr-plain {:tag :a, :class #"exter", :href #"github"}))))
  (is (= "Moustache"
         (text (find-it dr-plain [:div {:id "content"}, :a {:class "external"}]))))
  (is (= "Moustache"
         (text (find-it dr-plain [:div {:id "content"}, :a {:class #"exter"}]))))
  (is (= "Moustache"
         (text (find-it dr-plain [:div {:id "content"}, :a {:href #"github"}]))))
  (is (= "home"
         (text (find-it dr-plain [:* {:id "footer"}, :a {}]))))
  (is (= 8
         (count (find-them dr-plain {:tag :a}))))
  (is (= 3
         (count (find-them dr-plain {:class #"-item"}))))
  (is (= 3
         (count (find-them dr-plain {:tag :a, :class #"-item"}))))
  (is (= 1
         (count (find-them dr-plain {:tag :a, :text #"hom"}))))
  (is (= 1
         (count (find-them dr-plain {:tag :a, :text #"(?i)HOM"}))))
  (is (= 2
         (count (find-them dr-plain {:tag :a, :class #"exter", :href #"github"}))))
  (is (= 3
         (count (find-them dr-plain [:* {:id "footer"}, :a {}]))))
  (is (= 2
         (count (find-them dr-plain [:div {:id "content"}, :a {:class #"exter"}]))))
  (-> dr-plain
      (find-it {:tag :a, :text "example form"})
      click)
  (is (= "first_name"
         (attribute (find-it dr-plain {:type "text"}) "id")))
  (is (= "first_name"
         (attribute (find-it dr-plain {:tag :input, :type "text"}) "id")))
  (is (= "first_name"
         (attribute (find-it dr-plain {:tag :input, :type "text", :name "first_name"}) "id")))
  (is (= "first_name"
         (attribute (find-it dr-plain {:tag :input, :type "text", :name #"first_"}) "id")))
  (is (= "last_name"
         (attribute (find-it dr-plain {:tag :input, :type "text", :name #"last_"}) "id")))
  (is (= "Smith"
         (attribute (find-it dr-plain {:tag :input, :type "text", :name #"last_"}) "value")))
  (is (= "Smith"
         (attribute (find-it dr-plain {:tag :input, :type "text", :name #"last_"}) "value")))
  (is (= "Smith"
         (attribute (find-it dr-plain [:div {:id "content"}, :input {:name #"last_"}]) "value")))
  (back dr-plain) ;; get back to home page
  (is (-> dr-plain
        (find-it {:tag :a})
        exists?))
  (is (not
       (-> dr-plain
           (find-it {:tag :area})
           exists?)))
  (is (not
       (-> dr-plain
           (find-it {:tag :area})
           exists?)))
  (is (-> dr-plain 
          (find-it {:tag :a, :text "Moustache"})
          visible?))
  (is (-> dr-plain 
          (find-it {:tag :a, :text "Moustache"})
          displayed?))
  (is (-> dr-plain
          (find-it {:tag :a, :text "Moustache"})
          present?))
  (is (not
       (-> dr-plain
           (find-it {:tag :a})
           visible?)))
  (is (not
       (-> dr-plain 
           (find-it {:tag :a})
           displayed?)))
  (is (not
       (-> dr-plain
           (find-it {:tag :a})
           present?))))

(deftest plain-test-form-elements
  (to dr-plain (str test-base-url "example-form"))
  ;; Clear element
  (-> dr-plain
      (find-it [:form {:id "example_form"}, :input {:name #"last_"}])
      clear)
  (is (= ""
         (value (find-it dr-plain [:form {:id "example_form"}, :input {:name #"last_"}]))))
  ;; Radio buttons
  (is (= true
         (selected? (find-it dr-plain {:tag :input, :type "radio", :value "male"}))))
  (-> dr-plain
      (find-it {:tag :input, :type "radio", :value "female"})
      select)
  (is (= true
         (selected? (find-it dr-plain {:tag :input, :type "radio", :value "female"}))))
  (-> dr-plain
      (find-it {:tag :radio, :value "male"})
      select)
  (is (= true
         (selected? (find-it dr-plain {:tag :input, :type "radio", :value "male"}))))
  ;; Checkboxes
  (is (= false
         (selected? (find-it dr-plain {:tag :input, :type "checkbox", :name #"(?i)clojure"}))))
  (-> dr-plain
      (find-it {:tag :input, :type "checkbox", :name #"(?i)clojure"})
      toggle)
  (is (= true
         (selected? (find-it dr-plain {:tag :input, :type "checkbox", :name #"(?i)clojure"}))))
  (-> dr-plain
      (find-it {:tag :checkbox, :name #"(?i)clojure"})
      click)
  (is (= false
         (selected? (find-it dr-plain {:tag :input, :type "checkbox", :name #"(?i)clojure"}))))
  (-> dr-plain
      (find-it {:tag :checkbox, :type "checkbox", :name #"(?i)clojure"})
      select)
  (is (= true
         (selected? (find-it dr-plain {:tag :input, :type "checkbox", :name #"(?i)clojure"}))))
  ;; Text fields
  (is (= "true"
         (attribute (find-it dr-plain {:tag :input, :type "text", :value "Testing!"})
                    "readonly")))
  (-> dr-plain
      (find-it {:tag :input, :id "first_name"})
      (input-text "foobar"))
  (is (= "foobar"
         (value (find-it dr-plain {:tag :input, :id "first_name"}))))
  (-> dr-plain
      (find-it {:tag :textfield, :id "first_name"})
      clear
      (input-text "clojurian"))
  (is (= "clojurian"
         (value (find-it dr-plain {:tag :textfield, :id "first_name"})))))

(deftest plain-test-form-helpers
  (to dr-plain (str test-base-url "example-form"))
  (quick-fill dr-plain
              [{"first_name" clear}
               {"first_name" "Richard"}
               {{:id "last_name"} clear}
               {{:id "last_name"} "Hickey"}
               {{:name "bio"} clear}
               {{:name "bio"} #(input-text % "Creator of Clojure")}
               {{:tag "input", :type "radio", :value "female"} click}
               {{:css "select#countries"} #(select-by-value % "france")}])
  (is (= "Richard"
         (value (find-it dr-plain {:tag :input, :id "first_name"}))))
  (is (= "Hickey"
         (value (find-it dr-plain {:tag :input, :id "last_name"}))))
  (is (= "Creator of Clojure"
         (value (find-it dr-plain {:tag :textarea, :name "bio"}))))
  (is (selected?
       (find-it dr-plain {:tag :input, :type "radio", :value "female"})))
  (is (selected?
       (find-it dr-plain {:tag :option, :value "france"}))))

(deftest plain-test-window-handling
  (is (= 1
         (count (window-handles dr-plain))))
  (is (= "Ministache"
         (:title (window-handle dr-plain))))
  (-> dr-plain
      (find-it {:tag :a, :text "is amazing!"})
      click)
  (is (= "Ministache"
         (:title (window-handle dr-plain))))
  (is (= 2
         (count (window-handles dr-plain))))
  (switch-to-window dr-plain (second (window-handles dr-plain)))
  (is (= (str test-base-url "clojure")
         (:url (window-handle dr-plain))))
  (switch-to-other-window dr-plain)
  (is (= test-base-url
         (:url (window-handle dr-plain))))
  (-> dr-plain
      (switch-to-window (find-window dr-plain {:url (str test-base-url "clojure")})))
  (close dr-plain)
  (is (= test-base-url
         (:url (window-handle dr-plain)))))

(deftest plain-wait-until-should-wait-for-condition
  (is (= "Ministache" (title dr-plain)))
  (execute-script dr-plain "setTimeout(function () { window.document.title = \"asdf\"}, 3000)")
  (wait-until dr-plain (fn [d] (= "asdf" (title d))))
  (is (= "asdf" (title dr-plain))))

(deftest plain-wait-until-should-throw-on-timeout
  (is (thrown? TimeoutException
               (do
                 (execute-script dr-plain "setTimeout(function () { window.document.title = \"test\"}, 6000)")
                 (wait-until dr-plain (fn [d] (= "test" (title d))))))))

(deftest plain-wait-until-should-allow-timeout-argument
  (is (thrown? TimeoutException
               (do
                   (execute-script dr-plain "setTimeout(function () { window.document.title = \"test\"}, 10000)")
                   (wait-until dr-plain (fn [d] (= "test" (title d))) 1000)))))

(deftest plain-implicit-wait-should-cause-find-to-wait
  (implicit-wait dr-plain 3000)
  (execute-script dr-plain "setTimeout(function () { window.document.body.innerHTML = \"<div id='test'>hi!</div>\"}, 1000)")
  (is (= "test"
         (attribute (find-element dr-plain (by-id "test")) :id))))

;; Not sure how we'll test that flash in fact flashes,
;; but at least this will catch changing API's
(deftest plain-test-flash-helper
  (-> dr-plain
      (find-it {:tag :a, :text "Moustache"})
      flash))

;; Firefox-specific Functionality

(deftest firefox-should-support-custom-profiles
  (is (if (travis?)
        true
        (do
          (log/info "[x] Starting Firefox with custom profile.")
          (driver? (to (new-driver {:browser :firefox
                                    :profile (ff/new-profile)})
                       test-base-url))))))

(deftest firefox-should-support-extensions
  (is (if (travis?)
        true
        (do
          (log/info "[x] Starting Firefox with extensions.")
          (driver? (to (new-driver {:browser :firefox
                                    :profile (doto (ff/new-profile)
                                               (ff/enable-extension :firebug))})
                       test-base-url))))))

