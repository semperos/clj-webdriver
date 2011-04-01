(ns clj-webdriver.test.core
  (:require [clj-webdriver.test.example-app.core :as web-app])
  (:use [clj-webdriver.core] :reload)
  (:use ring.adapter.jetty)
  (:use [clojure.test]))

;; Setup
(def ^{:dynamic true} b (start :firefox "http://localhost:8080"))

(defn reset-browser-fixture
  [f]
  (f)
  (to b "http://localhost:8080"))

(defn quit-browser-fixture
  [f]
  (f)
  (quit b))

(use-fixtures :each reset-browser-fixture)
(use-fixtures :once quit-browser-fixture)

;; Tests
(deftest test-browser-basics
  (is (= org.openqa.selenium.firefox.FirefoxDriver (class b)))
  (is (= "http://localhost:8080/" (current-url b)))
  (is (= "Ministache" (title b)))
  (is (boolean (re-find #"(?i)<!DOCTYPE html>" (page-source b)))))

(deftest test-back-forward
  (-> b
      (find-them :a {:text "example form"})
      first
      click)
  (is (= "http://localhost:8080/example-form" (current-url b)))
  (back b)
  (is (= "http://localhost:8080/" (current-url b)))
  (forward b)
  (is (= "http://localhost:8080/example-form" (current-url b))))

(deftest test-to
  (to b "http://localhost:8080/example-form")
  (is (= "http://localhost:8080/example-form" (current-url b)))
  (is (= "Ministache" (title b))))

(deftest test-bys
  (-> b
      (find-them :a {:text "example form"})
      first
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
         (text (first (find-them b :a)))))
  (is (= "Moustache"
         (text (first (find-them b {:class "external"})))))
  (is (= "https://github.com/cgrand/moustache"
         (attribute (first (find-them b {:text "Moustache"})) "href")))
  (is (= "Moustache"
         (text (first (find-them b :a {:class #"exter"})))))
  (is (= "Moustache"
         (text (first (find-them b :a {:text #"Mous"})))))
  (is (= "Moustache"
         (text (first (find-them b :a {:class "external", :href #"github"})))))
  (is (= "Moustache"
         (text (first (find-them b :a {:class #"exter", :href #"github"})))))
  (is (= "Moustache"
         (text (first (find-them b [:div {:id "content"}, :a {:class "external"}])))))
  (is (= "Moustache"
         (text (first (find-them b [:div {:id "content"}, :a {:class #"exter"}])))))
  (is (= "Moustache"
         (text (first (find-them b [:div {:id "content"}, :a {:href #"github"}])))))
  (is (= "home"
         (text (first (find-them b [:* {:id "footer"}, :a {}])))))
  (is (= 6
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
      (find-them :a {:text "example form"})
      first
      click)
  (is (= "first_name"
         (attribute (first (find-them b {:type "text"})) "id")))
  (is (= "first_name"
         (attribute (first (find-them b :input {:type "text"})) "id")))
  (is (= "first_name"
         (attribute (first (find-them b :input {:type "text", :name "first_name"})) "id")))
  (is (= "first_name"
         (attribute (first (find-them b :input {:type "text", :name #"first_"})) "id")))
  (is (= "last_name"
         (attribute (first (find-them b :input {:type "text", :name #"last_"})) "id")))
  (is (= "Smith"
         (attribute (first (find-them b :input {:type "text", :name #"last_"})) "value")))
  (is (= "Smith"
         (attribute (first (find-them b :input {:type "text", :name #"last_"})) "value")))
  (is (= "Smith"
         (attribute (first (find-them b [:div {:id "content"}, :input {:name #"last_"}])) "value")))
  (is (true?
       (-> b
           (find-them :a)
           first
           exists?)))
  (is (false?
       (-> b
           (find-them :area)
           first
           exists?))))

(deftest test-form-elements
  (to b "http://localhost:8080/example-form")
  ;; Clear element
  (-> b
      (find-them [:form {:id "example_form"}, :input {:name #"last_"}])
      first
      clear)
  (is (= ""
         (value (first (find-them b [:form {:id "example_form"}, :input {:name #"last_"}])))))
  ;; Radio buttons
  (is (= true
         (selected? (first (find-them b :input {:type "radio", :value "male"})))))
  (-> b
      (find-them :input {:type "radio", :value "female"})
      first
      select)
  (is (= true
         (selected? (first (find-them b :input {:type "radio", :value "female"})))))
  (is (= false
         (selected? (first (find-them b :input {:type "radio", :value "male"})))))
  ;; Checkboxes
  (is (= false
         (selected? (first (find-them b :input {:type "checkbox", :name #"(?i)clojure"})))))
  (-> b
      (find-them :input {:type "checkbox", :name #"(?i)clojure"})
      first
      toggle)
  (is (= true
         (selected? (first (find-them b :input {:type "checkbox", :name #"(?i)clojure"})))))
  (-> b
      (find-them :input {:type "checkbox", :name #"(?i)clojure"})
      first
      click)
  (is (= false
         (selected? (first (find-them b :input {:type "checkbox", :name #"(?i)clojure"})))))
  (-> b
      (find-them :input {:type "checkbox", :name #"(?i)clojure"})
      first
      select)
  (is (= true
         (selected? (first (find-them b :input {:type "checkbox", :name #"(?i)clojure"})))))
  ;; Text fields
  (is (= "true"
         (attribute (first (find-them b :input {:type "text", :value "Testing!"}))
                    "readonly")))
  (-> b
      (find-them :input {:id "first_name"})
      first
      (input-text "foobar"))
  (is (= "foobar"
         (value (first (find-them b :input {:id "first_name"}))))))

(deftest test-window-handling
  (is (= 1
         (count (window-handles b))))
  (is (= "Ministache"
         (:title (window-handle b))))
  (-> b
      (find-them :a {:text "is amazing!"})
      first
      click)
  (is (= "Ministache"
         (:title (window-handle b))))
  (is (= 2
         (count (window-handles b))))
  (switch-to-window b (second (window-handles b)))
  (is (= "http://localhost:8080/clojure"
         (:url (window-handle b))))
  (switch-to-other-window b)
  (is (= "http://localhost:8080/"
         (:url (window-handle b))))
  (-> b
      (find-them :window {:url "http://localhost:8080/clojure"})
      first
      switch-to-window)
  (close b)
  (is (= "http://localhost:8080/"
         (:url (window-handle b)))))
;; TODO:
;;   * Form element tests (comprehensive)
;;   * Exception throwing (esp. for find-it/find-them argument handling)p