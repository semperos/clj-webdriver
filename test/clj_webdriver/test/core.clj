(ns clj-webdriver.test.core
  (:require [clj-webdriver.test.example-app.core :as web-app])
  (:use [clj-webdriver.core] :reload)
  (:use ring.adapter.jetty)
  (:use [clojure.test]))

(defn- start-test-browser
  []
  (start :firefox "http://localhost:8080"))

(deftest test-browser-basics
  (let [b (start-test-browser)]
    (is (= org.openqa.selenium.firefox.FirefoxDriver (class b)))
    (is (= "http://localhost:8080/" (current-url b)))
    (is (= "Ministache" (title b)))
    (is (boolean (re-find #"(?i)<!DOCTYPE html>" (page-source b))))
    (close b)))

(deftest test-back-forward
  (let [b (start-test-browser)]
    (do
      (-> b (find-it :a {:text "example form"}) click)
      (is (= "http://localhost:8080/example-form" (current-url b)))
      (back b)
      (is (= "http://localhost:8080/" (current-url b)))
      (forward b)
      (is (= "http://localhost:8080/example-form" (current-url b)))
      (close b))))

(deftest test-to
  (let [b (start-test-browser)]
    (do
      (to b "http://localhost:8080/example-form")
      (is (= "http://localhost:8080/example-form" (current-url b)))
      (is (= "Ministache" (title b)))
      (close b))))

(deftest test-bys
  (let [b (start-test-browser)]
    (do
      (-> b (find-it :a {:text "example form"}) click)
      (is (= "first_name"   (attribute (find-element b (by-id "first_name")) :id)))
      (is (= "home"         (text      (find-element b (by-link-text "home")))))
      (is (= "example form" (text      (find-element b (by-partial-link-text "example")))))
      (is (= "first_name"   (attribute (find-element b (by-name "first_name")) :id)))
      (is (= "home"         (text      (find-element b (by-tag-name "a")))))
      (is (= "home"         (text      (find-element b (by-xpath "//a[text()='home']")))))
      (is (= "home"         (text      (find-element b (by-class-name "menu-item")))))
      (is (= "home"         (text      (find-element b (by-css-selector "#footer a.menu-item")))))
      (close b))))

(deftest test-find-it-by-regex
  (let [b (start-test-browser)]
    (is (= "Moustache" (text (find-it b :a {:class #"exter"}))))
    (is (= "Moustache" (text (find-it b :a {:text #"Mous"}))))
    (close b)))
