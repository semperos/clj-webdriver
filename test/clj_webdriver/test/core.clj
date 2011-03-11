(ns clj-webdriver.test.core
  (:require [clj-webdriver.test.example-app.core :as web-app])
  (:use [clj-webdriver.core] :reload)
  (:use ring.adapter.jetty)
  (:use [clojure.test]))

(def b (start :firefox "http://localhost:8080"))

(deftest test-new-driver
  (is (= org.openqa.selenium.firefox.FirefoxDriver (class b))))

(deftest test-get-url
  (do
    (get-url b "http://localhost:8080/example-form")
    (is (= "http://localhost:8080/example-form" (.getCurrentUrl b)))))

(deftest test-current-url
  (do (back b)
      (is (= "http://localhost:8080/" (current-url b)))))

(deftest test-title
  (is (= "Ministache" (title b))))

(deftest test-page-source
  (is (boolean (re-find #"(?i)<!DOCTYPE html>" (page-source b)))))

(deftest test-back
  (do
    (forward b)
    (back b)
    (is (= "http://localhost:8080/" (current-url b)))))

(deftest test-forward
  (do
    (forward b)
    (is (= "http://localhost:8080/example-form"))))

(deftest test-to
  (do
    (to b "http://localhost:8080")
    (is (= "http://localhost:8080/" (current-url b)))
    (is (= "Ministache" (title b)))))

(deftest finish
  (do
    (close b)
    true))
