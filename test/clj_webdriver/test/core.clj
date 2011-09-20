(ns clj-webdriver.test.core
  (:require [clj-webdriver.test.example-app.core :as web-app])
  (:use [clj-webdriver.core] :reload)
  (:use [ring.adapter.jetty :only [run-jetty]])
  (:use [clojure.test])
  (:import [org.openqa.selenium TimeoutException]))

;; Setup
(def test-port 5744)
(def test-host "localhost")
(def test-base-url (str "http://" test-host ":" test-port "/"))
(def b (start :firefox test-base-url))

(defn start-server [f]
  (loop [server (run-jetty #'web-app/routes {:port test-port, :join? false})]
    (if (.isStarted server)
      (do
        (f)
        (.stop server))
      (recur server))))

(defn reset-browser-fixture
  [f]
  (to b test-base-url)
  (f))

(defn quit-browser-fixture
  [f]
  (f)
  (quit b))

(use-fixtures :once start-server quit-browser-fixture)
(use-fixtures :each reset-browser-fixture)

;; Tests
(deftest test-browser-basics
  (is (= org.openqa.selenium.firefox.FirefoxDriver (class b)))
  (is (= test-base-url (current-url b)))
  (is (= "Ministache" (title b)))
  (is (boolean (re-find #"(?i)<!DOCTYPE html>" (page-source b)))))

(deftest test-back-forward
  (-> b
      (find-it :a {:text "example form"})
      click)
  (is (= (str test-base-url "example-form") (current-url b)))
  (back b)
  (is (= test-base-url (current-url b)))
  (forward b)
  (is (= (str test-base-url "example-form") (current-url b))))

(deftest test-to
  (to b (str test-base-url "example-form"))
  (is (= (str test-base-url "example-form") (current-url b)))
  (is (= "Ministache" (title b))))

(deftest test-bys
  (-> b
      (find-it :a {:text "example form"})
      click)
  (is (= "first_name"
         (attribute (find-element b (by-id "first_name")) :id)))
  (is (= "home"
         (text (find-element b (by-link-text "home")))))
  (is (= "example form"
         (text (find-element b (by-partial-link-text "example")))))
  (is (= "first_name"
         (attribute (find-element b (by-name "first_name")) :id)))
  (is (= "home"
         (text (find-element b (by-tag-name "a")))))
  (is (= "home"
         (text (find-element b (by-xpath "//a[text()='home']")))))
  (is (= "home"
         (text (find-element b (by-class-name "menu-item")))))
  (is (= "home"
         (text (find-element b (by-css-selector "#footer a.menu-item"))))))

(deftest test-find*
  (is (= "Moustache"
         (text (find-it b :a))))
  (is (= "Moustache"
         (text (find-it b {:class "external"}))))
  (is (= "https://github.com/cgrand/moustache"
         (attribute (find-it b {:text "Moustache"}) "href")))
  (is (= "Moustache"
         (text (find-it b :a {:class #"exter"}))))
  (is (= "Moustache"
         (text (find-it b :a {:text #"Mous"}))))
  (is (= "Moustache"
         (text (find-it b :a {:class "external", :href #"github"}))))
  (is (= "Moustache"
         (text (find-it b :a {:class #"exter", :href #"github"}))))
  (is (= "Moustache"
         (text (find-it b [:div {:id "content"}, :a {:class "external"}]))))
  (is (= "Moustache"
         (text (find-it b [:div {:id "content"}, :a {:class #"exter"}]))))
  (is (= "Moustache"
         (text (find-it b [:div {:id "content"}, :a {:href #"github"}]))))
  (is (= "home"
         (text (find-it b [:* {:id "footer"}, :a {}]))))
  (is (= 7
         (count (find-them b :a))))
  (is (= 3
         (count (find-them b {:class #"-item"}))))
  (is (= 3
         (count (find-them b :a {:class #"-item"}))))
  (is (= 1
         (count (find-them b :a {:text #"hom"}))))
  (is (= 1
         (count (find-them b :a {:text #"(?i)HOM"}))))
  (is (= 2
         (count (find-them b :a {:class #"exter", :href #"github"}))))
  (is (= 3
         (count (find-them b [:* {:id "footer"}, :a {}]))))
  (is (= 2
         (count (find-them b [:div {:id "content"}, :a {:class #"exter"}]))))
  (-> b
      (find-it :a {:text "example form"})
      click)
  (is (= "first_name"
         (attribute (find-it b {:type "text"}) "id")))
  (is (= "first_name"
         (attribute (find-it b :input {:type "text"}) "id")))
  (is (= "first_name"
         (attribute (find-it b :input {:type "text", :name "first_name"}) "id")))
  (is (= "first_name"
         (attribute (find-it b :input {:type "text", :name #"first_"}) "id")))
  (is (= "last_name"
         (attribute (find-it b :input {:type "text", :name #"last_"}) "id")))
  (is (= "Smith"
         (attribute (find-it b :input {:type "text", :name #"last_"}) "value")))
  (is (= "Smith"
         (attribute (find-it b :input {:type "text", :name #"last_"}) "value")))
  (is (= "Smith"
         (attribute (find-it b [:div {:id "content"}, :input {:name #"last_"}]) "value")))
  (is (-> b
        (find-it :a)
        exists?))
  (is (not
       (-> b
           (find-it :area)
           exists?)))
  (is (nil?
       (-> b
           (find-it :area)
           exists?)))           
  (is (thrown? org.openqa.selenium.NoSuchElementException
               (find-it b :area))))

(deftest test-form-elements
  (to b (str test-base-url "example-form"))
  ;; Clear element
  (-> b
      (find-it [:form {:id "example_form"}, :input {:name #"last_"}])
      clear)
  (is (= ""
         (value (find-it b [:form {:id "example_form"}, :input {:name #"last_"}]))))
  ;; Radio buttons
  (is (= true
         (selected? (find-it b :input {:type "radio", :value "male"}))))
  (-> b
      (find-it :input {:type "radio", :value "female"})
      select)
  (is (= true
         (selected? (find-it b :input {:type "radio", :value "female"}))))
  (-> b
      (find-it :radio {:value "male"})
      select)
  (is (= true
         (selected? (find-it b :input {:type "radio", :value "male"}))))
  ;; Checkboxes
  (is (= false
         (selected? (find-it b :input {:type "checkbox", :name #"(?i)clojure"}))))
  (-> b
      (find-it :input {:type "checkbox", :name #"(?i)clojure"})
      toggle)
  (is (= true
         (selected? (find-it b :input {:type "checkbox", :name #"(?i)clojure"}))))
  (-> b
      (find-it :checkbox {:name #"(?i)clojure"})
      click)
  (is (= false
         (selected? (find-it b :input {:type "checkbox", :name #"(?i)clojure"}))))
  (-> b
      (find-it :checkbox {:type "checkbox", :name #"(?i)clojure"})
      select)
  (is (= true
         (selected? (find-it b :input {:type "checkbox", :name #"(?i)clojure"}))))
  ;; Text fields
  (is (= "true"
         (attribute (find-it b :input {:type "text", :value "Testing!"})
                    "readonly")))
  (-> b
      (find-it :input {:id "first_name"})
      (input-text "foobar"))
  (is (= "foobar"
         (value (find-it b :input {:id "first_name"}))))
  (-> b
      (find-it :textfield {:id "first_name"})
      clear
      (input-text "clojurian"))
  (is (= "clojurian"
         (value (find-it b :textfield {:id "first_name"})))))

(deftest test-window-handling
  (is (= 1
         (count (window-handles b))))
  (is (= "Ministache"
         (:title (window-handle b))))
  (-> b
      (find-it :a {:text "is amazing!"})
      click)
  (is (= "Ministache"
         (:title (window-handle b))))
  (is (= 2
         (count (window-handles b))))
  (switch-to-window b (second (window-handles b)))
  (is (= (str test-base-url "clojure")
         (:url (window-handle b))))
  (switch-to-other-window b)
  (is (= test-base-url
         (:url (window-handle b))))
  (-> b
      (find-it :window {:url (str test-base-url "clojure")})
      switch-to-window)
  (close b)
  (is (= test-base-url
         (:url (window-handle b)))))

(deftest wait-until-should-wait-for-condition
  (is (= "Ministache" (title b)))
  (doto b
    (execute-script "setTimeout(function () { window.document.title = \"asdf\"}, 3000)")
    (wait-until (fn [d] (= "asdf" (title d)))))
  (is (= "asdf" (title b))))

(deftest wait-until-should-throw-on-timeout
  (is (thrown? TimeoutException
               (doto b
                 (execute-script "setTimeout(function () { window.document.title = \"test\"}, 6000)")
                 (wait-until (fn [d] (= "test" (title d))))))))

(deftest wait-until-should-allow-timeout-argument
  (is (thrown? TimeoutException
               (doto b
                 (execute-script "setTimeout(function () { window.document.title = \"test\"}, 10000)")
                 (wait-until (fn [d] (= "test" (title d))) :timeout 1000)))))

(deftest implicit-wait-should-cause-find-to-wait
  (doto b
    (implicit-wait 3000)
    (execute-script "setTimeout(function () { window.document.body.innerHTML = \"<div id='test'>hi!</div>\"}, 1000)"))
  (is (= "test"
         (attribute (find-element b (by-id "test")) :id))))
