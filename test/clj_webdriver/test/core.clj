(ns clj-webdriver.test.core
  (:require [clj-webdriver.test.example-app.core :as web-app])
  (:use [clj-webdriver.core] :reload)
  (:use ring.adapter.jetty)
  (:use [clojure.test]))

(def *server* (run-jetty #'web-app/routes {:port 8080, :join? false}))
(def *driver* (start :firefox "http://localhost:8080"))

(deftest test-new-driver
  (is (= org.openqa.selenium.firefox.FirefoxDriver (class *driver*))))

(deftest test-get-url
  (do
    (get-url *driver* "http://localhost:8080/example-form")
    (is (= "http://localhost:8080/example-form" (.getCurrentUrl *driver*)))))

(back *driver*)

(deftest test-current-url
  (is (= "http://localhost:8080/" (current-url *driver*))))

(deftest test-title
  (is (= "Clojure Selenium-WebDriver Test Web App" (title *driver*))))

(deftest test-page-source
  (is (true? (re-find #"(?s)<!DOCTYPE html>.*<meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\" /><title>Clojure Selenium-WebDriver Test Web App</title>" (page-source *driver*)))))

(forward *driver*)

(deftest test-back
  (do
    (back *driver*)
    (is (= "http://localhost:8080/" (current-url *driver*)))))

(deftest test-forward
  (do
    (forward *driver*)
    (is (= "http://localhost:8080/example-form"))))

(deftest test-to
  (do
    (to *driver* "http://localhost:8080")
    (are (= "http://localhost:8080/" (current-url *driver*))
         (= "Clojure Selenium-WebDriver Test Web App" (title *driver*)))))

(close *driver*)
(.stop *server*)