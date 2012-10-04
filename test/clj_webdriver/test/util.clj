(ns clj-webdriver.test.util
  (:use [ring.adapter.jetty :only [run-jetty]]
        [clj-webdriver.test.config])
  (:require [clj-webdriver.test.example-app.core :as web-app]))

;; Fixtures
(defn start-server [f]
  (loop [server (run-jetty #'web-app/routes {:port test-port, :join? false})]
    (if (.isStarted server)
      (do
        (f)
        (.stop server))
      (recur server))))

;; Utilities
(defmacro thrown?
  "Return truthy if the exception in `klass` is thrown, otherwise return falsey (nil) (code adapted from clojure.test)"
  [klass & forms]
  `(try ~@forms
        false
        (catch ~klass e#
          true)))

(defn exclusive-between
  "Ensure a number is between a min and a max, both exclusive"
  [n min max]
  (and (> n min)
       (< n max)))