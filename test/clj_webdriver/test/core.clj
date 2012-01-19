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
  (to firefox-driver test-base-url)
  (to dr-plain test-base-url)
  (f))

(defn quit-browser-fixture
  [f]
  (f)
  (quit firefox-driver)
  (quit dr-plain))

(defn seed-driver-cache
  [f]
  (cache/seed firefox-driver {:url (current-url firefox-driver), {:query [:foo]} "bar"})
  (f))

(use-fixtures :once start-server quit-browser-fixture)
(use-fixtures :each reset-browser-fixture seed-driver-cache)



;; ## Cache-Based Tests ##
(deftest test-browser-basics
  (is (= clj_webdriver.driver.Driver (class firefox-driver)))
  (is (= test-base-url (current-url firefox-driver)))
  (is (= "Ministache" (title firefox-driver)))
  (is (boolean (re-find #"(?i)<!DOCTYPE html>" (page-source firefox-driver)))))

(deftest back-forward-should-traverse-browser-history
  (-> firefox-driver
      (find-it {:tag :a, :text "example form"})
      click)
  (is (= (str test-base-url "example-form") (current-url firefox-driver)))
  (back firefox-driver)
  (is (= test-base-url (current-url firefox-driver)))
  (forward firefox-driver)
  (is (= (str test-base-url "example-form") (current-url firefox-driver))))

(deftest to-should-open-given-url-in-browser
  (to firefox-driver (str test-base-url "example-form"))
  (is (= (str test-base-url "example-form") (current-url firefox-driver)))
  (is (= "Ministache" (title firefox-driver))))

(deftest should-be-able-to-find-elements-using-low-level-by-wrappers
  (-> firefox-driver
      (find-it {:tag :a, :text "example form"})
      click)
  (is (= "first_name"
         (attribute (find-element firefox-driver (by-id "first_name")) :id)))
  (is (= "home"
         (text (find-element firefox-driver (by-link-text "home")))))
  (is (= "example form"
         (text (find-element firefox-driver (by-partial-link-text "example")))))
  (is (= "first_name"
         (attribute (find-element firefox-driver (by-name "first_name")) :id)))
  (is (= "home"
         (text (find-element firefox-driver (by-tag "a")))))
  (is (= "home"
         (text (find-element firefox-driver (by-xpath "//a[text()='home']")))))
  (is (= "home"
         (text (find-element firefox-driver (by-class-name "menu-item")))))
  (is (= "home"
         (text (find-element firefox-driver (by-css-selector "#footer a.menu-item")))))
  (to firefox-driver test-base-url)
  (is (= "first odd"
         (attribute (find-element firefox-driver (by-class-name "first odd")) :class))))

(deftest find-it-should-support-basic-attr-val-map
  (is (= "Moustache"
         (text (nth (find-them firefox-driver {:tag :a}) 1))))
  (is (= "Moustache"
         (text (find-it firefox-driver {:class "external"}))))
  (is (= "first odd"
         (attribute (find-it firefox-driver {:class "first odd"}) :class)))
  (is (= "first odd"
         (attribute (find-it firefox-driver {:tag :li, :class "first odd"}) :class)))
  (is (= "https://github.com/cgrand/moustache"
         (attribute (find-it firefox-driver {:text "Moustache"}) "href")))
  (is (= 8
         (count (find-them firefox-driver {:tag :a}))))
  (-> firefox-driver
      (find-it {:tag :a, :text "example form"})
      click)
  (is (= "first_name"
         (attribute (find-it firefox-driver {:type "text"}) "id")))
  (is (= "first_name"
         (attribute (find-it firefox-driver {:tag :input, :type "text"}) "id")))
  (is (= "first_name"
         (attribute (find-it firefox-driver {:tag :input, :type "text", :name "first_name"}) "id")))
  (is (= "first_name"
         (attribute (find-it firefox-driver {:tag :input, :type "text", :name #"first_"}) "id")))
  (is (= "last_name"
         (attribute (find-it firefox-driver {:tag :input, :type "text", :name #"last_"}) "id")))
  (is (= "Smith"
         (attribute (find-it firefox-driver {:tag :input, :type "text", :name #"last_"}) "value")))
  (is (= "Smith"
         (attribute (find-it firefox-driver {:tag :input, :type "text", :name #"last_"}) "value"))))

(deftest find-it-should-support-regexes-in-attr-val-map
  (is (= "Moustache"
         (text (find-it firefox-driver {:tag :a, :class #"exter"}))))
  (is (= "Moustache"
         (text (find-it firefox-driver {:tag :a, :text #"Mous"}))))
  (is (= "Moustache"
         (text (find-it firefox-driver {:tag :a, :class "external", :href #"github"}))))
  (is (= "Moustache"
         (text (find-it firefox-driver {:tag :a, :class #"exter", :href #"github"}))))
  (is (= 3
         (count (find-them firefox-driver {:class #"-item"}))))
  (is (= 3
         (count (find-them firefox-driver {:tag :a, :class #"-item"}))))
  (is (= 1
         (count (find-them firefox-driver {:tag :a, :text #"hom"}))))
  (is (= 1
         (count (find-them firefox-driver {:tag :a, :text #"(?i)HOM"}))))
  (is (= 2
         (count (find-them firefox-driver {:tag :a, :class #"exter", :href #"github"})))))

(deftest find-it-should-support-hierarchical-querying
  (is (= "Moustache"
         (text (find-it firefox-driver [{:tag :div, :id "content"}, {:tag :a, :class "external"}]))))
  (is (= "Moustache"
         (text (find-it firefox-driver [{:tag :div, :id "content"}, {:tag :a, :class #"exter"}]))))
  (is (= "Moustache"
         (text (find-it firefox-driver [{:tag :div, :id "content"}, {:tag :a, :href #"github"}]))))
  (is (= "home"
         (text (find-it firefox-driver [{:tag :*, :id "footer"}, {:tag :a}]))))
  (is (= 3
         (count (find-them firefox-driver [{:tag :*, :id "footer"}, {:tag :a}]))))
  (is (= 2
         (count (find-them firefox-driver [{:tag :div, :id "content"}, {:tag :a, :class #"exter"}])))))

(deftest exists-should-return-truthy-falsey-and-should-not-throw-an-exception
  (is (-> firefox-driver
        (find-it {:tag :a})
        exists?))
  (is (not
       (-> firefox-driver
           (find-it {:tag :area})
           exists?))))

(deftest visible-should-return-truthy-falsey-when-visible
  (is (-> firefox-driver 
          (find-it {:tag :a, :text "Moustache"})
          visible?))
  (is (not
       (-> firefox-driver
           (find-it {:tag :a, :href "#pages"})
           visible?)))
  (is (-> firefox-driver 
          (find-it {:tag :a, :text "Moustache"})
          displayed?))
  (is (not
       (-> firefox-driver
           (find-it {:tag :a, :href "#pages"})
           displayed?))))

(deftest present-should-return-truthy-falsey-when-exists-and-visible
  (is (-> firefox-driver
          (find-it {:tag :a, :text "Moustache"})
          present?))
  (is (not
       (-> firefox-driver
           (find-it {:tag :a, :href "#pages"})
           present?))))

;; Default wrap for strings is double quotes
(deftest generated-xpath-should-wrap-strings-in-double-quotes
  (is (find-it firefox-driver {:text "File's Name"})))

(deftest xpath-function-should-return-string-xpath-of-element
  (is (= (xpath (find-it firefox-driver {:tag :a, :text "Moustache"})) "/html/body/div[2]/div/p/a")))

(deftest html-function-should-return-string-html-of-element
  (is (= (html (find-it firefox-driver {:tag :a, :text "Moustache"})) "<a xmlns=\"http://www.w3.org/1999/xhtml\" class=\"external\" href=\"https://github.com/cgrand/moustache\">Moustache</a>")))

(deftest find-table-cell-should-find-cell-with-coords
  (is (= "th"
         (tag (find-table-cell firefox-driver {:id "pages-table"} [0 0]))))
  (is (= "th"
         (tag (find-table-cell firefox-driver {:id "pages-table"} [0 1]))))
  (is (= "td"
         (tag (find-table-cell firefox-driver {:id "pages-table"} [1 0]))))
  (is (= "td"
         (tag (find-table-cell firefox-driver {:id "pages-table"} [1 1])))))

(deftest find-table-row-should-find-all-cells-for-row
  (is (= 2
         (count (find-table-row firefox-driver {:id "pages-table"} 0))))
  (is (= "th"
         (tag (first (find-table-row firefox-driver {:id "pages-table"} 0)))))
  (is (= "td"
         (tag (first (find-table-row firefox-driver {:id "pages-table"} 1))))))

(deftest test-form-elements
  (to firefox-driver (str test-base-url "example-form"))
  ;; Clear element
  (-> firefox-driver
      (find-it [{:tag :form, :id "example_form"}, {:tag :input, :name #"last_"}])
      clear)
  (is (= ""
         (value (find-it firefox-driver [{:tag :form, :id "example_form"}, {:tag :input, :name #"last_"}]))))
  ;; Radio buttons
  (is (= true
         (selected? (find-it firefox-driver {:tag :input, :type "radio", :value "male"}))))
  (-> firefox-driver
      (find-it {:tag :input, :type "radio", :value "female"})
      select)
  (is (= true
         (selected? (find-it firefox-driver {:tag :input, :type "radio", :value "female"}))))
  (-> firefox-driver
      (find-it {:tag :radio, :value "male"})
      select)
  (is (= true
         (selected? (find-it firefox-driver {:tag :input, :type "radio", :value "male"}))))
  ;; Checkboxes
  (is (= false
         (selected? (find-it firefox-driver {:tag :input, :type "checkbox", :name #"(?i)clojure"}))))
  (-> firefox-driver
      (find-it {:tag :input, :type "checkbox", :name #"(?i)clojure"})
      toggle)
  (is (= true
         (selected? (find-it firefox-driver {:tag :input, :type "checkbox", :name #"(?i)clojure"}))))
  (-> firefox-driver
      (find-it {:tag :checkbox, :name #"(?i)clojure"})
      click)
  (is (= false
         (selected? (find-it firefox-driver {:tag :input, :type "checkbox", :name #"(?i)clojure"}))))
  (-> firefox-driver
      (find-it {:tag :checkbox, :type "checkbox", :name #"(?i)clojure"})
      select)
  (is (= true
         (selected? (find-it firefox-driver {:tag :input, :type "checkbox", :name #"(?i)clojure"}))))
  ;; Text fields
  (-> firefox-driver
      (find-it {:tag :input, :id "first_name"})
      (input-text "foobar"))
  (is (= "foobar"
         (value (find-it firefox-driver {:tag :input, :id "first_name"}))))
  (-> firefox-driver
      (find-it {:tag :textfield, :id "first_name"})
      clear
      (input-text "clojurian"))
  (is (= "clojurian"
         (value (find-it firefox-driver {:tag :textfield, :id "first_name"}))))
  ;; Boolean attributes (disabled, readonly, etc)
  (is (= "disabled"
         (attribute (find-it firefox-driver {:id "disabled_field"}) :disabled)))
  (is (= "readonly"
         (attribute (find-it firefox-driver {:id "purpose_here"}) :readonly)))
  (is (nil?
       (attribute (find-it firefox-driver {:id "disabled_field"}) :readonly)))
  (is (nil?
       (attribute (find-it firefox-driver {:id "purpose_here"}) :disabled))))

(deftest quick-fill-should-accept-special-seq-and-perform-batch-actions-on-form
  (to firefox-driver (str test-base-url "example-form"))
  (quick-fill firefox-driver
              [{"first_name" clear}
               {"first_name" "Richard"}
               {{:id "last_name"} clear}
               {{:id "last_name"} "Hickey"}
               {{:name "bio"} clear}
               {{:name "bio"} #(input-text % "Creator of Clojure")}
               {{:tag "input", :type "radio", :value "female"} click}
               {{:css "select#countries"} #(select-by-value % "france")}])
  (is (= "Richard"
         (value (find-it firefox-driver {:tag :input, :id "first_name"}))))
  (is (= "Hickey"
         (value (find-it firefox-driver {:tag :input, :id "last_name"}))))
  (is (= "Creator of Clojure"
         (value (find-it firefox-driver {:tag :textarea, :name "bio"}))))
  (is (selected?
       (find-it firefox-driver {:tag :input, :type "radio", :value "female"})))
  (is (selected?
       (find-it firefox-driver {:tag :option, :value "france"}))))

(deftest quick-fill-submit-should-always-return-nil
  (to firefox-driver (str test-base-url "example-form"))
  (is (nil?
       (quick-fill-submit firefox-driver
                   [{"first_name" clear}
                    {"first_name" "Richard"}
                    {{:id "last_name"} clear}
                    {{:id "last_name"} "Hickey"}
                    {{:name "bio"} clear}
                    {{:name "bio"} #(input-text % "Creator of Clojure")}
                    {{:tag "input", :type "radio", :value "female"} click}
                    {{:css "select#countries"} #(select-by-value % "france")}]))))

(deftest should-be-able-to-toggle-between-open-windows
  (is (= 1
         (count (window-handles firefox-driver))))
  (is (= "Ministache"
         (:title (window-handle firefox-driver))))
  (-> firefox-driver
      (find-it {:tag :a, :text "is amazing!"})
      click)
  (is (= "Ministache"
         (:title (window-handle firefox-driver))))
  (is (= 2
         (count (window-handles firefox-driver))))
  (switch-to-window firefox-driver (second (window-handles firefox-driver)))
  (is (= (str test-base-url "clojure")
         (:url (window-handle firefox-driver))))
  (switch-to-other-window firefox-driver)
  (is (= test-base-url
         (:url (window-handle firefox-driver))))
  (-> firefox-driver
      (switch-to-window (find-window firefox-driver {:url (str test-base-url "clojure")})))
  (close firefox-driver)
  (is (= test-base-url
         (:url (window-handle firefox-driver)))))

(deftest wait-until-should-wait-for-condition
  (is (= "Ministache" (title firefox-driver)))
  (execute-script firefox-driver "setTimeout(function () { window.document.title = \"asdf\"}, 3000)")
  (wait-until firefox-driver (fn [d] (= "asdf" (title d))))
  (is (= "asdf" (title firefox-driver))))

(deftest wait-until-should-throw-on-timeout
  (is (thrown? TimeoutException
               (do
                 (execute-script firefox-driver "setTimeout(function () { window.document.title = \"test\"}, 6000)")
                 (wait-until firefox-driver (fn [d] (= "test" (title d))))))))

(deftest wait-until-should-allow-timeout-argument
  (is (thrown? TimeoutException
               (do
                   (execute-script firefox-driver "setTimeout(function () { window.document.title = \"test\"}, 10000)")
                   (wait-until firefox-driver (fn [d] (= (title d) "test")) 1000)))))

(deftest implicit-wait-should-cause-find-to-wait
  (implicit-wait firefox-driver 3000)
  (execute-script firefox-driver "setTimeout(function () { window.document.body.innerHTML = \"<div id='test'>hi!</div>\"}, 1000)")
  (is (= "test"
         (attribute (find-element firefox-driver (by-id "test")) :id))))

;; Not sure how we'll test that flash in fact flashes,
;; but at least this will catch changing API's
(deftest test-flash-helper
  (-> firefox-driver
      (find-it {:tag :a, :text "Moustache"})
      flash))

;; Caching
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
         (text (find-it dr-plain [{:tag :div, :id "content"}, {:tag :a, :class "external"}]))))
  (is (= "Moustache"
         (text (find-it dr-plain [{:tag :div, :id "content"}, {:tag :a, :class #"exter"}]))))
  (is (= "Moustache"
         (text (find-it dr-plain [{:tag :div, :id "content"}, {:tag :a, :href #"github"}]))))
  (is (= "home"
         (text (find-it dr-plain [{:tag :*, :id "footer"}, {:tag :a}]))))
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
         (count (find-them dr-plain [{:tag :*, :id "footer"}, {:tag :a}]))))
  (is (= 2
         (count (find-them dr-plain [{:tag :div, :id "content"}, {:tag :a, :class #"exter"}]))))
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
         (attribute (find-it dr-plain [{:tag :div, :id "content"}, {:tag :input, :name #"last_"}]) "value")))
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
      (find-it [{:tag :form, :id "example_form"}, {:tag :input, :name #"last_"}])
      clear)
  (is (= ""
         (value (find-it dr-plain [{:tag :form, :id "example_form"}, {:tag :input, :name #"last_"}]))))
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
         (value (find-it dr-plain {:tag :textfield, :id "first_name"}))))
  ;; Boolean attributes (disabled, readonly, etc.)
  (is (= "disabled"
         (attribute (find-it dr-plain {:id "disabled_field"}) :disabled)))
  (is (= "readonly"
         (attribute (find-it dr-plain {:id "purpose_here"}) :readonly)))
  (is (nil?
       (attribute (find-it dr-plain {:id "disabled_field"}) :readonly)))
  (is (nil?
       (attribute (find-it dr-plain {:id "purpose_here"}) :disabled))))

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

