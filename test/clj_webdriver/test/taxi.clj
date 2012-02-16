(ns clj-webdriver.test.taxi
  (:use clj-webdriver.taxi
        clj-webdriver.test.config
        [ring.adapter.jetty :only [run-jetty]]
        midje.sweet)
  (:require [clj-webdriver.core :as core]
            [clj-webdriver.test.example-app.core :as web-app]))

(defonce test-server (run-jetty #'web-app/routes {:port test-port, :join? false}))
(def driver {:browser :firefox})
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
  (Thread/sleep 500)
  (fact (current-url) => (str test-base-url "example-form"))
  (back)
  (fact (current-url) => test-base-url)
  (forward)
  (fact (current-url) => (str test-base-url "example-form"))
  
  ;; To function (navigation)
  (to (str test-base-url "example-form"))
  (facts
   (current-url) => (str test-base-url "example-form")
   (title) => "Ministache")

  ;; Find capability, CSS default
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
   (attribute (find-element-by (core/by-class-name "first odd")) :class) => "first odd"
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
  (go "example-form")
  (facts
   (attribute "*[type='text']" :id) => "first_name"
   (attribute "input[type='text']" :id) => "first_name"
   (attribute "input[type='text'][name='first_name']" :id) => "first_name"
   (attribute "input[type='text'][name^='first_']" :id) => "first_name"
   (attribute (find-element {:tag :input, :type "text", :name #"first_"}) :id) => "first_name"
   (attribute (find-element {:tag :input, :type "text", :name #"last_"}) :id) => "last_name"
   (attribute (find-element {:tag :input, :type "text", :name #"last_"}) :value) => "Smith"

   ;; Stopped at find-element-should-support-regexes-in-attr-val-map
   ))



;; Find capabilitiy, XPath default
(with-driver-fn driver xpath-finder
  (go "example-form")
  (facts
   (text "//a[text()='home']") => "home"
   (text "//a[text()='example form']") => "example form"
   (text "//a[text()='home']") => "home")
  (go)
  (facts
   (attribute "//*[text()='Moustache']" :href) => "https://github.com/cgrand/moustache"

   ))

(.stop test-server)