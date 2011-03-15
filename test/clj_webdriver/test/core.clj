(ns clj-webdriver.test.core
  (:require [clj-webdriver.test.example-app.core :as web-app])
  (:use [clj-webdriver.core] :reload)
  (:use ring.adapter.jetty)
  (:use [clojure.test]))

;; Setup
(def ^{:dynamic true} *b* (start :firefox "http://localhost:8080"))

(defn reset-browser-fixture
  [f]
  (f)
  (to *b* "http://localhost:8080"))

(defn close-browser-fixture
  [f]
  (f)
  (close *b*))

(use-fixtures :each reset-browser-fixture)
(use-fixtures :once close-browser-fixture)

;; Tests
(deftest test-browser-basics
  (is (= org.openqa.selenium.firefox.FirefoxDriver (class *b*)))
  (is (= "http://localhost:8080/" (current-url *b*)))
  (is (= "Ministache" (title *b*)))
  (is (boolean (re-find #"(?i)<!DOCTYPE html>" (page-source *b*)))))

(deftest test-back-forward
  (do
    (-> *b* (find-it :a {:text "example form"}) click)
    (is (= "http://localhost:8080/example-form" (current-url *b*)))
    (back *b*)
    (is (= "http://localhost:8080/" (current-url *b*)))
    (forward *b*)
    (is (= "http://localhost:8080/example-form" (current-url *b*)))))

(deftest test-to
  (do
    (to *b* "http://localhost:8080/example-form")
    (is (= "http://localhost:8080/example-form" (current-url *b*)))
    (is (= "Ministache" (title *b*)))))

(deftest test-bys
  (do
    (-> *b* (find-it :a {:text "example form"}) click)
    (is (= "first_name"   (attribute (find-element *b* (by-id "first_name")) :id)))
    (is (= "home"         (text      (find-element *b* (by-link-text "home")))))
    (is (= "example form" (text      (find-element *b* (by-partial-link-text "example")))))
    (is (= "first_name"   (attribute (find-element *b* (by-name "first_name")) :id)))
    (is (= "home"         (text      (find-element *b* (by-tag-name "a")))))
    (is (= "home"         (text      (find-element *b* (by-xpath "//a[text()='home']")))))
    (is (= "home"         (text      (find-element *b* (by-class-name "menu-item")))))
    (is (= "home"         (text      (find-element *b* (by-css-selector "#footer a.menu-item")))))))

(deftest test-find*
  (is (= "Moustache" (text (find-it *b* :a {:class #"exter"}))))
  (is (= "Moustache" (text (find-it *b* :a {:text #"Mous"}))))
  (is (= "Moustache" (text (find-it *b* :a {:class #"exter", :href #"github"}))))
  (is (= "Moustache" (text (find-it *b* [:div {:id "content"}, :a {:class "external"}]))))
  (is (= "Moustache" (text (find-it *b* [:div {:id "content"}, :a {:class #"exter"}]))))
  (is (= "home"      (text (find-it *b* [:* {:id "footer"}, :a {}]))))
  (is (= 2           (count (find-them *b* :a {:class #"-item"}))))
  (is (= 1           (count (find-them *b* :a {:text #"hom"}))))
  (is (= 2           (count (find-them *b* :a {:class #"exter", :href #"github"})))))